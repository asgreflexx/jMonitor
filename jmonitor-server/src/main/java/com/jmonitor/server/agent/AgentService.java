package com.jmonitor.server.agent;

import com.jmonitor.common.dto.AgentStatus;
import com.jmonitor.common.dto.MethodHotspot;
import com.jmonitor.server.process.JvmConnectionManager;
import com.sun.tools.attach.VirtualMachine;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the jMonitor instrumentation agent into a target JVM and reads back its
 * method-timing hotspots over JMX (Phase 6).
 *
 * <p>The shaded agent jar is bundled as a classpath resource; it is extracted
 * once to a temp file so the Attach API can load it by path.
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final ObjectName PROFILER = profilerName();

    private final JvmConnectionManager connections;
    private Path agentJar;

    public AgentService(JvmConnectionManager connections) {
        this.connections = connections;
    }

    @PostConstruct
    void extractAgentJar() throws IOException {
        ClassPathResource resource = new ClassPathResource("agent/jmonitor-agent.jar");
        if (!resource.exists()) {
            log.warn("Bundled agent jar not found on classpath; agent features disabled");
            return;
        }
        Path temp = Files.createTempFile("jmonitor-agent", ".jar");
        temp.toFile().deleteOnExit();
        try (InputStream in = resource.getInputStream(); OutputStream out = Files.newOutputStream(temp)) {
            StreamUtils.copy(in, out);
        }
        this.agentJar = temp;
        log.info("Extracted instrumentation agent to {}", temp);
    }

    public AgentStatus status(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        try {
            if (!conn.isRegistered(PROFILER)) {
                return new AgentStatus(false, null, 0);
            }
            String prefix = (String) conn.getAttribute(PROFILER, "InstrumentedPrefix");
            int classes = (int) conn.getAttribute(PROFILER, "InstrumentedClassCount");
            return new AgentStatus(true, prefix, classes);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot read agent status: " + e.getMessage(), e);
        }
    }

    /** Attaches and loads the agent, instrumenting classes under {@code prefix}. */
    public void load(long pid, String prefix) throws IOException {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("An instrumentation prefix is required");
        }
        if (prefix.startsWith("java.") || prefix.startsWith("jdk.")
                || prefix.startsWith("sun.") || prefix.startsWith("com.sun.")) {
            throw new IllegalArgumentException("Refusing to instrument core package: " + prefix);
        }
        if (connections.isSelf(pid)) {
            throw new IllegalArgumentException("Cannot instrument the jMonitor server itself");
        }
        if (agentJar == null) {
            throw new IOException("Agent jar is not available");
        }
        if (status(pid).loaded()) {
            throw new IllegalArgumentException("Agent already loaded in pid " + pid);
        }

        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(Long.toString(pid));
            vm.loadAgent(agentJar.toAbsolutePath().toString(), prefix.trim());
            log.info("Loaded instrumentation agent into pid {} (prefix {})", pid, prefix);
        } catch (Exception e) {
            throw new IOException("Failed to load agent: " + e.getMessage(), e);
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException ignore) {
                    // best effort
                }
            }
        }
    }

    public List<MethodHotspot> hotspots(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        try {
            if (!conn.isRegistered(PROFILER)) {
                throw new IllegalArgumentException("Agent is not loaded in pid " + pid);
            }
            String dump = (String) conn.getAttribute(PROFILER, "Hotspots");
            return parse(dump);
        } catch (IOException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot read hotspots: " + e.getMessage(), e);
        }
    }

    public void reset(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        try {
            conn.invoke(PROFILER, "reset", new Object[0], new String[0]);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Cannot reset agent: " + e.getMessage(), e);
        }
    }

    private static List<MethodHotspot> parse(String dump) {
        List<MethodHotspot> result = new ArrayList<>();
        if (dump == null || dump.isBlank()) {
            return result;
        }
        for (String line : dump.split("\n")) {
            String[] parts = line.split("\t");
            if (parts.length != 3) {
                continue;
            }
            try {
                result.add(new MethodHotspot(parts[0],
                        Long.parseLong(parts[1]), Long.parseLong(parts[2])));
            } catch (NumberFormatException ignore) {
                // skip malformed line
            }
        }
        return result;
    }

    private static ObjectName profilerName() {
        try {
            return ObjectName.getInstance("com.jmonitor:type=MethodProfiler");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
