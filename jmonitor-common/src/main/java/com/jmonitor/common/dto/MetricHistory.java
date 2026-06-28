package com.jmonitor.common.dto;

import java.util.List;
import java.util.Map;

/**
 * A consolidated time-series slice read back from the archive (Phase 3).
 *
 * <p>{@code timestamps} and each series in {@code series} are aligned and equal
 * in length. Series values may be {@code NaN} where no data was recorded.
 *
 * @param pid         the process the history belongs to
 * @param fromMillis  inclusive start of the requested window, epoch millis
 * @param toMillis    inclusive end of the requested window, epoch millis
 * @param stepMillis  resolution of the returned points, in millis
 * @param timestamps  point timestamps, epoch millis
 * @param series      metric name -> values, e.g. {@code "heapUsed" -> [...]}
 */
public record MetricHistory(
        long pid,
        long fromMillis,
        long toMillis,
        long stepMillis,
        long[] timestamps,
        Map<String, double[]> series
) {
}
