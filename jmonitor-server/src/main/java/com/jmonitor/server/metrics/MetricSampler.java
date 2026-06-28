package com.jmonitor.server.metrics;

import com.jmonitor.common.dto.GcStat;
import com.jmonitor.common.dto.MemoryPoolStat;
import com.jmonitor.common.dto.MetricSnapshot;
import com.jmonitor.server.process.JvmConnectionManager;
import org.springframework.stereotype.Component;

import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@link MetricSnapshot} from a target JVM's platform MXBeans over JMX.
 *
 * <p>Uses {@code com.sun.management.OperatingSystemMXBean} for per-process CPU
 * load, which the plain {@code java.lang.management} variant does not expose.
 */
@Component
public class MetricSampler {

    private final JvmConnectionManager connections;

    public MetricSampler(JvmConnectionManager connections) {
        this.connections = connections;
    }

    public MetricSnapshot sample(long pid) throws IOException {
        MBeanServerConnection c = connections.getConnection(pid);

        MemoryMXBean memory = ManagementFactory.getPlatformMXBean(c, MemoryMXBean.class);
        ThreadMXBean threads = ManagementFactory.getPlatformMXBean(c, ThreadMXBean.class);
        ClassLoadingMXBean classes = ManagementFactory.getPlatformMXBean(c, ClassLoadingMXBean.class);
        com.sun.management.OperatingSystemMXBean os =
                ManagementFactory.getPlatformMXBean(c, com.sun.management.OperatingSystemMXBean.class);
        List<GarbageCollectorMXBean> gcBeans =
                ManagementFactory.getPlatformMXBeans(c, GarbageCollectorMXBean.class);
        List<MemoryPoolMXBean> poolBeans =
                ManagementFactory.getPlatformMXBeans(c, MemoryPoolMXBean.class);

        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        long gcCount = 0;
        long gcTime = 0;
        List<GcStat> gcStats = new ArrayList<>(gcBeans.size());
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = Math.max(0, gc.getCollectionCount());
            long time = Math.max(0, gc.getCollectionTime());
            gcCount += count;
            gcTime += time;
            gcStats.add(new GcStat(gc.getName(), count, time));
        }

        List<MemoryPoolStat> poolStats = new ArrayList<>(poolBeans.size());
        for (MemoryPoolMXBean pool : poolBeans) {
            MemoryUsage usage = pool.getUsage();
            long used = usage == null ? 0 : usage.getUsed();
            long committed = usage == null ? 0 : usage.getCommitted();
            long max = usage == null ? -1 : usage.getMax();
            poolStats.add(new MemoryPoolStat(pool.getName(), pool.getType().name(), used, committed, max));
        }

        return new MetricSnapshot(
                pid,
                System.currentTimeMillis(),
                heap.getUsed(), heap.getCommitted(), heap.getMax(),
                nonHeap.getUsed(), nonHeap.getCommitted(),
                os.getProcessCpuLoad(), os.getCpuLoad(), os.getSystemLoadAverage(),
                threads.getThreadCount(), threads.getDaemonThreadCount(),
                threads.getPeakThreadCount(), threads.getTotalStartedThreadCount(),
                classes.getLoadedClassCount(), classes.getTotalLoadedClassCount(),
                classes.getUnloadedClassCount(),
                gcCount, gcTime,
                gcStats, poolStats
        );
    }
}
