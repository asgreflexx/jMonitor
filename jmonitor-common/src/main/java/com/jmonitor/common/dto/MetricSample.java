package com.jmonitor.common.dto;

/**
 * A single time-stamped metric reading for a monitored JVM.
 *
 * <p>This is the unit that flows through the live WebSocket stream (Phase 2) and
 * is persisted into the time-series database (Phase 3).
 *
 * @param pid             the process the sample belongs to
 * @param name            metric key, e.g. {@code "heap.used"}, {@code "cpu.process"}
 * @param value           the numeric reading
 * @param epochMillis     capture time in milliseconds since the epoch
 */
public record MetricSample(
        long pid,
        String name,
        double value,
        long epochMillis
) {
}
