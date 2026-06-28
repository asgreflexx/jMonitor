package com.jmonitor.server.jfr;

import com.jmonitor.common.dto.JfrRecordingInfo;
import com.jmonitor.server.process.JvmConnectionManager;
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

    /** pid -> active recording id and its profile name. */
    private final Map<Long, long[]> active = new ConcurrentHashMap<>();
    private final Map<Long, String> activeProfile = new ConcurrentHashMap<>();

    public JfrService(JvmConnectionManager connections,
                      JfrRegistry registry,
                      @Value("${jmonitor.data-dir:${user.home}/.jmonitor/rrd}") String dataDir) {
        this.connections = connections;
        this.registry = registry;
        this.recordingsDir = Path.of(dataDir).resolveSibling("jfr");
    }

    public boolean isRecording(long pid) {
        return active.containsKey(pid);
    }

    public String activeProfile(long pid) {
        return activeProfile.get(pid);
    }

    /** Starts a recording with the given predefined profile ("default" or "profile"). */
    public void start(long pid, String profile) throws IOException {
        if (!ALLOWED_PROFILES.contains(profile)) {
            throw new IllegalArgumentException("Unknown JFR profile: " + profile);
        }
        if (active.containsKey(pid)) {
            throw new IllegalArgumentException("A recording is already running for pid " + pid);
        }
        FlightRecorderMXBean jfr = bean(pid);
        try {
            long id = jfr.newRecording();
            jfr.setPredefinedConfiguration(id, profile);
            jfr.startRecording(id);
            active.put(pid, new long[]{id});
            activeProfile.put(pid, profile);
            log.info("Started JFR recording {} (profile {}) on pid {}", id, profile, pid);
        } catch (Exception e) {
            throw new IOException("Failed to start JFR recording: " + e.getMessage(), e);
        }
    }

    /** Stops the active recording, copies the .jfr to the server and registers it. */
    public JfrRecordingInfo stop(long pid) throws IOException {
        long[] handle = active.remove(pid);
        String profile = activeProfile.remove(pid);
        if (handle == null) {
            throw new IllegalArgumentException("No active recording for pid " + pid);
        }
        long id = handle[0];
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
            jfr.closeRecording(id);
            if (!Files.exists(target)) {
                throw new IOException("JFR file was not created at " + target);
            }
            log.info("Stopped JFR recording {} on pid {} -> {}", id, pid, fileName);
            return registry.insert(pid, fileName, Files.size(target), now,
                    profile == null ? "unknown" : profile);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to stop JFR recording: " + e.getMessage(), e);
        }
    }

    public Path resolveRecordingFile(String fileName) {
        Path p = recordingsDir.resolve(fileName).normalize();
        if (!p.startsWith(recordingsDir)) {
            throw new IllegalArgumentException("Invalid recording file name: " + fileName);
        }
        return p;
    }

    private FlightRecorderMXBean bean(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        return ManagementFactory.getPlatformMXBean(conn, FlightRecorderMXBean.class);
    }
}
