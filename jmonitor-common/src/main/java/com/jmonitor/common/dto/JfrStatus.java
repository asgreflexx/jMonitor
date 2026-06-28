package com.jmonitor.common.dto;

/**
 * Whether a JFR recording is currently active for a process (Phase 5).
 *
 * @param recording true if a recording is running
 * @param profile   the active configuration name, or null when not recording
 */
public record JfrStatus(
        boolean recording,
        String profile
) {
}
