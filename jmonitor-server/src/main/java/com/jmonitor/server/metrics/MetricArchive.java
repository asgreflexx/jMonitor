package com.jmonitor.server.metrics;

import com.jmonitor.common.dto.MetricHistory;
import com.jmonitor.common.dto.MetricSnapshot;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists {@link MetricSnapshot}s into per-process RRD4J round-robin databases
 * and serves consolidated history slices back for charting (Phase 3).
 *
 * <p>Each pid gets one {@code .rrd} file with a fixed set of gauges and several
 * roll-up archives (5&nbsp;min raw, 1&nbsp;h, 1&nbsp;d, 1&nbsp;w averages). RRD4J
 * picks the best-resolution archive for a given fetch window automatically.
 *
 * <p><b>Note:</b> files are keyed by pid only; the OS may reuse a pid after a
 * process exits, so a fresh process can continue an old file. Acceptable for the
 * current scope — a pid+start-time key can be introduced later if needed.
 */
@Component
public class MetricArchive {

    private static final Logger log = LoggerFactory.getLogger(MetricArchive.class);

    private static final int STEP_SECONDS = 1;

    /**
     * Archived datasources mapped to their snapshot accessor. RRD4J limits
     * datasource names to 20 characters, hence the short keys.
     */
    private static final Map<String, java.util.function.ToDoubleFunction<MetricSnapshot>> DATASOURCES =
            new LinkedHashMap<>();

    static {
        DATASOURCES.put("heapUsed", s -> s.heapUsed());
        DATASOURCES.put("heapCommit", s -> s.heapCommitted());
        DATASOURCES.put("heapMax", s -> s.heapMax() < 0 ? Double.NaN : s.heapMax());
        DATASOURCES.put("nonHeapUsed", s -> s.nonHeapUsed());
        DATASOURCES.put("cpuProcess", s -> s.processCpuLoad() < 0 ? Double.NaN : s.processCpuLoad());
        DATASOURCES.put("cpuSystem", s -> s.systemCpuLoad() < 0 ? Double.NaN : s.systemCpuLoad());
        DATASOURCES.put("threads", s -> s.threadCount());
        DATASOURCES.put("classes", s -> s.loadedClassCount());
        DATASOURCES.put("gcCount", s -> s.gcCount());
        DATASOURCES.put("gcTime", s -> s.gcTimeMillis());
    }

    private final Path dataDir;
    private final Map<Long, RrdDb> open = new ConcurrentHashMap<>();

    public MetricArchive(@Value("${jmonitor.data-dir:${user.home}/.jmonitor/rrd}") String dataDir) {
        this.dataDir = Path.of(dataDir);
    }

    /** Records one snapshot. Silently no-ops (with a debug log) on I/O errors. */
    public void record(MetricSnapshot snapshot) {
        try {
            RrdDb db = dbFor(snapshot.pid(), snapshot.epochMillis() / 1000);
            synchronized (db) {
                long timeSec = snapshot.epochMillis() / 1000;
                if (timeSec <= db.getLastUpdateTime()) {
                    return; // RRD4J rejects non-increasing timestamps
                }
                Sample sample = db.createSample(timeSec);
                for (var entry : DATASOURCES.entrySet()) {
                    sample.setValue(entry.getKey(), entry.getValue().applyAsDouble(snapshot));
                }
                sample.update();
            }
        } catch (IOException e) {
            log.debug("Archiving pid {} failed: {}", snapshot.pid(), e.getMessage());
        }
    }

    /**
     * Fetches consolidated AVERAGE history for the given window. When
     * {@code metrics} is empty all datasources are returned.
     */
    public MetricHistory fetch(long pid, long fromMillis, long toMillis, List<String> metrics)
            throws IOException {
        RrdDb db = open.get(pid);
        if (db == null && Files.exists(rrdPath(pid))) {
            db = dbFor(pid, fromMillis / 1000);
        }
        if (db == null) {
            return new MetricHistory(pid, fromMillis, toMillis, STEP_SECONDS * 1000L,
                    new long[0], Map.of());
        }

        FetchData data;
        synchronized (db) {
            data = db.createFetchRequest(ConsolFun.AVERAGE, fromMillis / 1000, toMillis / 1000)
                    .fetchData();
        }

        long[] secs = data.getTimestamps();
        long[] timestamps = new long[secs.length];
        for (int i = 0; i < secs.length; i++) {
            timestamps[i] = secs[i] * 1000L;
        }

        List<String> wanted = metrics.isEmpty() ? List.copyOf(DATASOURCES.keySet()) : metrics;
        Map<String, double[]> series = new LinkedHashMap<>();
        for (String name : wanted) {
            if (DATASOURCES.containsKey(name)) {
                series.put(name, data.getValues(name));
            }
        }

        long stepMillis = data.getStep() * 1000L;
        return new MetricHistory(pid, fromMillis, toMillis, stepMillis, timestamps, series);
    }

    public void evict(long pid) {
        RrdDb db = open.remove(pid);
        if (db != null) {
            try {
                db.close();
            } catch (IOException ignore) {
                // already closed
            }
        }
    }

    private RrdDb dbFor(long pid, long startTimeSec) throws IOException {
        RrdDb existing = open.get(pid);
        if (existing != null) {
            return existing;
        }
        synchronized (open) {
            existing = open.get(pid);
            if (existing != null) {
                return existing;
            }
            Files.createDirectories(dataDir);
            Path path = rrdPath(pid);
            RrdDb db = Files.exists(path)
                    ? RrdDb.getBuilder().setPath(path.toString()).build()
                    : RrdDb.getBuilder().setRrdDef(define(path, startTimeSec)).build();
            open.put(pid, db);
            return db;
        }
    }

    private Path rrdPath(long pid) {
        return dataDir.resolve(pid + ".rrd");
    }

    private static RrdDef define(Path path, long startTimeSec) {
        RrdDef def = new RrdDef(path.toString(), startTimeSec - 1, STEP_SECONDS);
        // GAUGE: store values as-is; heartbeat = 2 steps marks a gap as NaN.
        for (String name : DATASOURCES.keySet()) {
            def.addDatasource(name, DsType.GAUGE, 2L * STEP_SECONDS, Double.NaN, Double.NaN);
        }
        // AVERAGE roll-ups: 5 min @1s, 1 h @10s, 1 d @60s, 1 w @5min.
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 300);
        def.addArchive(ConsolFun.AVERAGE, 0.5, 10, 360);
        def.addArchive(ConsolFun.AVERAGE, 0.5, 60, 1440);
        def.addArchive(ConsolFun.AVERAGE, 0.5, 300, 2016);
        return def;
    }

    @PreDestroy
    void closeAll() {
        open.values().forEach(db -> {
            try {
                db.close();
            } catch (IOException ignore) {
                // shutting down
            }
        });
        open.clear();
    }
}
