package com.jmonitor.server.diagnostics;

import com.jmonitor.common.dto.ThreadDump;
import com.jmonitor.common.dto.ThreadInfoDto;
import com.jmonitor.server.process.JvmConnectionManager;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Captures full thread dumps (with locks and deadlock detection) from a target
 * JVM via its {@link ThreadMXBean} over JMX (Phase 4).
 */
@Service
public class ThreadDumpService {

    private final JvmConnectionManager connections;

    public ThreadDumpService(JvmConnectionManager connections) {
        this.connections = connections;
    }

    public ThreadDump capture(long pid) throws IOException {
        MBeanServerConnection conn = connections.getConnection(pid);
        ThreadMXBean threads = ManagementFactory.getPlatformMXBean(conn, ThreadMXBean.class);

        long[] deadlocked = threads.findDeadlockedThreads();
        Set<Long> deadlockedSet = deadlocked == null
                ? Set.of()
                : Arrays.stream(deadlocked).boxed().collect(Collectors.toSet());

        ThreadInfo[] infos = threads.dumpAllThreads(true, true);
        List<ThreadInfoDto> result = new ArrayList<>(infos.length);
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            result.add(toDto(info, deadlockedSet.contains(info.getThreadId())));
        }

        List<Long> deadlockedIds = deadlockedSet.stream().sorted().toList();
        return new ThreadDump(pid, System.currentTimeMillis(), result, deadlockedIds);
    }

    private static ThreadInfoDto toDto(ThreadInfo info, boolean deadlocked) {
        List<String> stack = new ArrayList<>();
        for (StackTraceElement frame : info.getStackTrace()) {
            stack.add(frame.toString());
        }
        return new ThreadInfoDto(
                info.getThreadId(),
                info.getThreadName(),
                info.getThreadState().name(),
                stack,
                info.getLockName(),
                info.getLockOwnerId(),
                info.getLockOwnerName(),
                info.getBlockedCount(),
                info.getWaitedCount(),
                info.isInNative(),
                info.isSuspended(),
                deadlocked
        );
    }
}
