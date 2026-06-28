package com.jmonitor.common.dto;

/**
 * An active threshold breach for a monitored JVM (Phase 7).
 *
 * @param level     "WARNING" or "CRITICAL"
 * @param metric    the metric that breached, e.g. "heap" or "cpu"
 * @param message   human-readable description
 * @param value     the observed value (percent)
 * @param threshold the threshold that was crossed (percent)
 */
public record Alert(
        String level,
        String metric,
        String message,
        double value,
        double threshold
) {
}
