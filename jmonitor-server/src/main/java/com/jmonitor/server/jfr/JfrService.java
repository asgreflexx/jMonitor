package com.jmonitor.server.jfr;

import com.jmonitor.common.dto.JfrRecordingInfo;
import com.jmonitor.common.dto.JfrStatus;
import com.jmonitor.server.process.JvmConnectionManager;
import com.jmonitor.server.support.SafePaths;
import jdk.management.jfr.FlightRecorderMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives Java Flight Recorder on a target JVM via {@link FlightRecorderMXBean}
 * over JMX (Phase 5): start a recording with a predefined configuration, stop it
 * and copy the {@code .jfr} to the server for analysis/download.
 *
 * <p>The active recording id per pid is tracked in memory (one recording per
 * process at a time). The {@code .jfr} is written by the target JVM into the
 * server's recordings directory — the same local-user assumption as heap dumps.
 */
@Service
public class JfrService {

    private static final Logger log = LoggerFactory.getLogger(JfrService.class);
    private static final Set<String> ALLOWED_PROFILES = Set.of("default", "profile");

    private final JvmConnectionManager connections;
    private final JfrRegistry registry;
    private final Path recordingsDir;

    /** pid -> the single active recording for that process. */
    private final Map<Long, ActiveRecording> active = new ConcurrentHashMap<>();

    public JfrService(JvmConnectionManager connections,
                      JfrRegistry registry,
                      @Value("${jmonitor.data-dir:${user.home}/.jmonitor/rrd}") String dataDir) {
        this.connections = connections;
        this.registry = registry;
        this.recordingsDir = Path.of(dataDir).resolveSibling("jfr");
    }

    public JfrStatus status(long pid) {
        ActiveRecording rec = active.get(pid);
        return rec == null ? new JfrStatus(false, null) : new JfrStatus(true, rec.profile());
    }

    /**
     * Starts a recording with the given predefined profile ("default" or
     * "profile"). Synchronized so a concurrent double-start can't create and
     * leak two recordings on the target.
     */
    public synchronized void start(long pid, String profile) throws IOException {
        if (!ALLOWED_PROFILES.contains(profile)) {
            throw new IllegalArgumentException("Unknown JFR profile: " + profile);
        }
        if (active.containsKey(pid)) {
            throw new IllegalArgumentException("A recording is already running for pid " + pid);
        }
        FlightRecorderMXBean jfr = bean(pid);
        long id;
        try {
            id = jfr.newRecording();
            jfr.setPredefinedConfiguration(id, profile);
            jfr.startRecording(id);
        } catch (Exception e) {
            throw new IOException("Failed to start JFR recording: " + e.getMessage(), e);
        }
        active.put(pid, new ActiveRecording(id, profile));
        log.info("Started JFR recording {} (profile {}) on pid {}", id, profile, pid);
    }

    /**
     * Stops the active recording, copies the .jfr to the server and registers
     * it. Synchronized with {@link #start}. The recording is always closed and
     * its tracking removed (in the finally block) even on failure, so a failed
     * copy can never leak an open recording on the target.
     */
    public synchronized JfrRecordingInfo stop(long pid) throws IOException {
        ActiveRecording rec = active.get(pid);
        if (rec == null) {
            throw new IllegalArgumentException("No active recording for pid " + pid);
        }
        long id = rec.id();
        FlightRecorderMXBean jfr = bean(pid);
        try {
            jfr.stopRecording(id);
            Files.createDirectories(recordingsDir);
            long now = System.currentTimeMillis();
            String fileName = pid + "-" + now + ".jfr";
            Path target = recordingsDir.resolve(fileName);
            for (int n = 1; Files.exists(target); n++) {
                fileName = pid + "-" + now + "-" + n + ".jfr";
                target = recordingsDir.resolve(fileName);
            }
            jfr.copyTo(id, target.toAbsolutePath().toString());
            if (!Files.exists(target)) {
                throw new IOException("JFR file was not created at " + target);
            }
            log.info("Stopped JFR recording {} on pid {} -> {}", id, pid, fileName);
            return registry.insert(pid, fileName, Files.size(target), now, rec.profile());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to stop JFR recording: " + e.getMessage(), e);
        } finally {
            // Always close the target recording and drop tracking — never leak.
            try {
                jfr.closeRecording(id);
            } catch (Exception ignore) {
                // target may already have closed it
            }
            active.remove(pid);
        }
    }

    public Path resolveRecordingFile(String fileName) {
        return SafePaths.resolveWithin(recordingsDir, fileName);
    }

    /** The single active recording per process: JFR recording id + its profile. */
    private record ActiveRecording(long id, String profile) {
    }

    private FlightRecorderMXBean bean(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        return ManagementFactory.getPlatformMXBean(conn, FlightRecorderMXBean.class);
    }
}
