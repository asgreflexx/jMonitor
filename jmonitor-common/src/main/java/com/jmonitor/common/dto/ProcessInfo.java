package com.jmonitor.common.dto;

/**
 * Lightweight description of a locally discovered JVM process.
 *
 * <p>Populated from the Attach API ({@code VirtualMachine.list()}) in Phase 1.
 * Kept dependency-free so it can be reused by the server and the future agent.
 *
 * @param pid         the operating-system process id
 * @param displayName the main class or jar reported by the JVM, may be blank
 * @param attachable  whether the JVM exposes an attach mechanism we can connect to
 */
public record ProcessInfo(
        long pid,
        String displayName,
        boolean attachable
) {
}
