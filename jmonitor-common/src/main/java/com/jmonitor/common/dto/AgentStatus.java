package com.jmonitor.common.dto;

/**
 * Whether the jMonitor instrumentation agent is loaded in a target JVM (Phase 6).
 *
 * @param loaded                 true if the agent's profiler MBean is present
 * @param prefix                 the instrumented name prefix, or null if not loaded
 * @param instrumentedClassCount number of classes transformed so far
 */
public record AgentStatus(
        boolean loaded,
        String prefix,
        int instrumentedClassCount
) {
}
