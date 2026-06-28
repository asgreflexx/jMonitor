package com.jmonitor.common.dto;

/**
 * Usage of a single memory pool at sample time.
 *
 * @param name      pool name, e.g. "G1 Eden Space"
 * @param type      "HEAP" or "NON_HEAP"
 * @param used      bytes currently used
 * @param committed bytes committed by the JVM
 * @param max       maximum bytes, or -1 if undefined
 */
public record MemoryPoolStat(
        String name,
        String type,
        long used,
        long committed,
        long max
) {
}
