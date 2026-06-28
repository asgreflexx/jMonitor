package com.jmonitor.server.diagnostics;

import com.jmonitor.common.dto.HeapDumpInfo;
import com.jmonitor.common.dto.HeapHistogram;
import com.jmonitor.server.process.JvmConnectionManager;
import com.jmonitor.server.support.SafePaths;
import com.sun.management.HotSpotDiagnosticMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heap diagnostics for a target JVM (Phase 4):
 * <ul>
 *   <li>class histogram via the {@code GC.class_histogram} diagnostic command, and</li>
 *   <li>heap dumps via {@link HotSpotDiagnosticMXBean#dumpHeap}.</li>
 * </ul>
 *
 * <p>Heap dumps are written by the <em>target</em> JVM to a path under the
 * server's dump directory; since both run as the same local user this file is
 * then readable by the server for download.
 */
@Service
public class HeapService {

    private static final ObjectName DIAGNOSTIC_COMMAND = diagnosticCommandName();

    // Matches lines like:  "   1:        12345        9876544  java.lang.String"
    private static final Pattern HISTO_ROW =
            Pattern.compile("^\\s*(\\d+):\\s+(\\d+)\\s+(\\d+)\\s+(\\S+).*$");

    private final JvmConnectionManager connections;
    private final HeapDumpRegistry registry;
    private final Path dumpDir;

    public HeapService(JvmConnectionManager connections,
                       HeapDumpRegistry registry,
                       @Value("${jmonitor.data-dir:${user.home}/.jmonitor/rrd}") String dataDir) {
        this.connections = connections;
        this.registry = registry;
        // Sibling "heapdumps" directory next to the time-series data.
        this.dumpDir = Path.of(dataDir).resolveSibling("heapdumps");
    }

    public HeapHistogram histogram(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        String text;
        try {
            text = (String) conn.invoke(DIAGNOSTIC_COMMAND, "gcClassHistogram",
                    new Object[]{new String[0]}, new String[]{String[].class.getName()});
        } catch (Exception e) {
            throw new IOException("Class histogram failed: " + e.getMessage(), e);
        }
        return parse(pid, text);
    }

    public HeapDumpInfo dumpHeap(long pid, boolean live) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        HotSpotDiagnosticMXBean diagnostic =
                ManagementFactory.getPlatformMXBean(conn, HotSpotDiagnosticMXBean.class);

        Files.createDirectories(dumpDir);
        long now = System.currentTimeMillis();

        // dumpHeap requires the file not to exist yet; ensure a unique name even
        // for rapid successive dumps of the same pid or a stale leftover file.
        String fileName = pid + "-" + now + ".hprof";
        Path target = dumpDir.resolve(fileName);
        for (int n = 1; Files.exists(target); n++) {
            fileName = pid + "-" + now + "-" + n + ".hprof";
            target = dumpDir.resolve(fileName);
        }

        try {
            diagnostic.dumpHeap(target.toAbsolutePath().toString(), live);
        } catch (Exception e) {
            throw new IOException("Heap dump failed: " + e.getMessage(), e);
        }

        if (!Files.exists(target)) {
            // The target reported success but we can't see the file (e.g. a
            // different filesystem view) — don't record a phantom 0-byte dump.
            throw new IOException("Heap dump file was not created at " + target
                    + " (target JVM may not share this filesystem)");
        }
        return registry.insert(pid, fileName, Files.size(target), now, live);
    }

    public Path resolveDumpFile(String fileName) {
        return SafePaths.resolveWithin(dumpDir, fileName);
    }

    private static HeapHistogram parse(long pid, String text) {
        List<HeapHistogram.Row> rows = new ArrayList<>();
        long totalInstances = 0;
        long totalBytes = 0;
        for (String line : text.split("\\R")) {
            Matcher m = HISTO_ROW.matcher(line);
            if (!m.matches()) {
                continue;
            }
            int rank = Integer.parseInt(m.group(1));
            long instances = Long.parseLong(m.group(2));
            long bytes = Long.parseLong(m.group(3));
            String className = m.group(4);
            rows.add(new HeapHistogram.Row(rank, instances, bytes, className));
            totalInstances += instances;
            totalBytes += bytes;
        }
        return new HeapHistogram(pid, System.currentTimeMillis(), totalInstances, totalBytes, rows);
    }

    private static ObjectName diagnosticCommandName() {
        try {
            return ObjectName.getInstance("com.sun.management:type=DiagnosticCommand");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
