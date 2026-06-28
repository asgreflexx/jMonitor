package com.jmonitor.common.dto;

/**
 * Per-collector garbage-collection statistics at sample time.
 *
 * @param name                 the GC name, e.g. "G1 Young Generation"
 * @param collectionCount      total collections so far (cumulative)
 * @param collectionTimeMillis total time spent collecting, ms (cumulative)
 */
public record GcStat(
        String name,
        long collectionCount,
        long collectionTimeMillis
) {
}
