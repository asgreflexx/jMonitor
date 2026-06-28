package com.jmonitor.server.process;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages JMX connections to locally discovered JVMs.
 *
 * <p>For each target the JDK Attach API is used to start the JVM's local
 * management agent ({@link VirtualMachine#startLocalManagementAgent()}, JDK&nbsp;11+),
 * yielding a JMX service URL we connect to. Open connectors are cached per pid
 * and reused; dead connectors are evicted lazily on failure and proactively for
 * processes that have exited (see {@link #retainOnly(Set)}).
 */
@Component
public class JvmConnectionManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JvmConnectionManager.class);

    private final Map<Long, JMXConnector> connectors = new ConcurrentHashMap<>();
    private final long ownPid = ProcessHandle.current().pid();

    /** The pid of the jMonitor server itself, which we never attach to. */
    public boolean isSelf(long pid) {
        return pid == ownPid;
    }

    /**
     * Returns a live {@link MBeanServerConnection} for the given pid, attaching
     * and starting the target's management agent on first use.
     *
     * @throws IOException if the JVM cannot be attached to or has gone away
     */
    public MBeanServerConnection getConnection(long pid) throws IOException {
        if (isSelf(pid)) {
            throw new IOException("Refusing to attach to the jMonitor server's own JVM (pid " + pid + ")");
        }
        JMXConnector cached = connectors.get(pid);
        if (cached != null) {
            try {
                return cached.getMBeanServerConnection();
            } catch (IOException stale) {
                log.debug("Stale JMX connection for pid {}, reconnecting", pid);
                evict(pid);
            }
        }
        return connect(pid).getMBeanServerConnection();
    }

    private synchronized JMXConnector connect(long pid) throws IOException {
        JMXConnector existing = connectors.get(pid);
        if (existing != null) {
            return existing;
        }
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(Long.toString(pid));
            String address = vm.startLocalManagementAgent();
            JMXServiceURL url = new JMXServiceURL(address);
            JMXConnector connector = JMXConnectorFactory.connect(url);
            connectors.put(pid, connector);
            log.info("Connected to JVM pid {} via {}", pid, address);
            return connector;
        } catch (AttachNotSupportedException e) {
            throw new IOException("Attach not supported for pid " + pid + ": " + e.getMessage(), e);
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException ignore) {
                    // detaching is best-effort; the JMX connector keeps working independently
                }
            }
        }
    }

    /**
     * Attaches to the target, loads a java-agent and detaches. Centralises the
     * Attach API dance so callers don't reimplement it.
     *
     * @throws IOException if attach or agent loading fails
     */
    public void loadAgent(long pid, String agentJarPath, String options) throws IOException {
        if (isSelf(pid)) {
            throw new IllegalArgumentException("Cannot attach jMonitor to its own JVM (pid " + pid + ")");
        }
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(Long.toString(pid));
            vm.loadAgent(agentJarPath, options);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to load agent into pid " + pid + ": " + e.getMessage(), e);
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

    /** Closes and forgets the connector for a single pid. */
    public void evict(long pid) {
        JMXConnector c = connectors.remove(pid);
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
                // already gone
            }
        }
    }

    /** Evicts cached connectors for any pid no longer in the alive set. */
    public void retainOnly(Set<Long> alivePids) {
        for (Long pid : new ArrayList<>(connectors.keySet())) {
            if (!alivePids.contains(pid)) {
                log.info("Process {} has exited, closing its JMX connection", pid);
                evict(pid);
            }
        }
    }

    @Override
    public void close() {
        new ArrayList<>(connectors.keySet()).forEach(this::evict);
    }
}
