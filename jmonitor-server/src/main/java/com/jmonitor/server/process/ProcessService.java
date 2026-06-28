package com.jmonitor.server.process;

import com.jmonitor.common.dto.JvmDetails;
import com.jmonitor.common.dto.ProcessInfo;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Discovers locally running JVMs (Attach API) and reads their runtime details
 * over JMX.
 */
@Service
public class ProcessService {

    private final JvmConnectionManager connections;

    public ProcessService(JvmConnectionManager connections) {
        this.connections = connections;
    }

    /**
     * Lists all JVMs visible to the current user. Also reaps cached connections
     * for processes that have since exited.
     */
    public List<ProcessInfo> listProcesses() {
        Set<Long> alive = new HashSet<>();
        List<ProcessInfo> result = new ArrayList<>();

        for (VirtualMachineDescriptor d : VirtualMachine.list()) {
            long pid;
            try {
                pid = Long.parseLong(d.id());
            } catch (NumberFormatException e) {
                continue;
            }
            alive.add(pid);
            // We cannot attach to our own JVM; everything else is attempted on demand.
            boolean attachable = !connections.isSelf(pid);
            result.add(new ProcessInfo(pid, displayName(d), attachable));
        }

        connections.retainOnly(alive);
        result.sort(Comparator.comparing(ProcessInfo::displayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /**
     * Reads detailed runtime information for a single JVM via its RuntimeMXBean.
     *
     * @throws IOException if the process cannot be attached to or has gone away
     */
    public JvmDetails details(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        RuntimeMXBean runtime = ManagementFactory.getPlatformMXBean(conn, RuntimeMXBean.class);
        Map<String, String> props = runtime.getSystemProperties();

        return new JvmDetails(
                pid,
                props.getOrDefault("sun.java.command", ""),
                runtime.getVmName(),
                runtime.getVmVendor(),
                runtime.getVmVersion(),
                props.getOrDefault("java.runtime.version", props.getOrDefault("java.version", "")),
                props.getOrDefault("java.home", ""),
                runtime.getStartTime(),
                runtime.getUptime(),
                List.copyOf(runtime.getInputArguments()),
                new TreeMap<>(props)
        );
    }

    private static String displayName(VirtualMachineDescriptor d) {
        String name = d.displayName();
        return (name == null || name.isBlank()) ? "(unknown)" : name;
    }
}
