package com.jmonitor.common.dto;

import java.util.List;

/**
 * A class-level heap histogram (Phase 4), produced via the JVM's
 * {@code GC.class_histogram} diagnostic command.
 *
 * @param pid            the process the histogram was taken from
 * @param epochMillis    capture time, epoch millis
 * @param totalInstances total instance count across all classes
 * @param totalBytes     total retained bytes across all classes
 * @param rows           per-class rows, largest first
 */
public record HeapHistogram(
        long pid,
        long epochMillis,
        long totalInstances,
        long totalBytes,
        List<Row> rows
) {
    /**
     * One histogram row.
     *
     * @param rank      1-based size rank
     * @param instances number of instances of the class
     * @param bytes     bytes occupied by those instances
     * @param className the class name
     */
    public record Row(int rank, long instances, long bytes, String className) {
    }
}
