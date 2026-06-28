package com.jmonitor.common.dto;

import java.util.List;

/**
 * A complete reading of a JVM's core platform metrics at one instant.
 *
 * <p>Produced once per sampling tick (Phase 2), streamed live over WebSocket and
 * kept in a short in-memory ring buffer for chart backfill.
 *
 * <p>CPU loads are fractions in {@code [0.0, 1.0]}, or {@code -1} when the JVM
 * cannot supply them yet.
 *
 * @param pid                   the process the snapshot belongs to
 * @param epochMillis           capture time, epoch millis
 * @param heapUsed              heap bytes in use
 * @param heapCommitted         heap bytes committed
 * @param heapMax               heap max bytes, or -1 if undefined
 * @param nonHeapUsed           non-heap bytes in use
 * @param nonHeapCommitted      non-heap bytes committed
 * @param processCpuLoad        recent CPU load of this JVM process, [0..1] or -1
 * @param systemCpuLoad         recent CPU load of the whole machine, [0..1] or -1
 * @param systemLoadAverage     OS load average for the last minute, or -1
 * @param threadCount           live threads
 * @param daemonThreadCount     live daemon threads
 * @param peakThreadCount       peak live threads since JVM start
 * @param totalStartedThreadCount total threads started since JVM start
 * @param loadedClassCount      classes currently loaded
 * @param totalLoadedClassCount classes loaded since JVM start
 * @param unloadedClassCount    classes unloaded since JVM start
 * @param gcCount               total collections across all collectors
 * @param gcTimeMillis          total GC time across all collectors, ms
 * @param garbageCollectors     per-collector stats
 * @param memoryPools           per-pool usage
 */
public record MetricSnapshot(
        long pid,
        long epochMillis,
        long heapUsed,
        long heapCommitted,
        long heapMax,
        long nonHeapUsed,
        long nonHeapCommitted,
        double processCpuLoad,
        double systemCpuLoad,
        double systemLoadAverage,
        int threadCount,
        int daemonThreadCount,
        int peakThreadCount,
        long totalStartedThreadCount,
        int loadedClassCount,
        long totalLoadedClassCount,
        long unloadedClassCount,
        long gcCount,
        long gcTimeMillis,
        List<GcStat> garbageCollectors,
        List<MemoryPoolStat> memoryPools
) {
}
