package com.jmonitor.common.dto;

/**
 * Aggregated timing for one instrumented method (Phase 6 agent).
 *
 * @param method     the method signature, e.g. {@code "com.acme.Service.handle"}
 * @param calls      number of recorded invocations
 * @param totalNanos total wall-clock time across all invocations, nanoseconds
 */
public record MethodHotspot(
        String method,
        long calls,
        long totalNanos
) {
}
