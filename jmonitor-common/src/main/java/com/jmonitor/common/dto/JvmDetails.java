package com.jmonitor.common.dto;

import java.util.List;
import java.util.Map;

/**
 * Detailed information about a single connected JVM, read from its
 * {@link java.lang.management.RuntimeMXBean} over JMX (Phase 1).
 *
 * @param pid               the operating-system process id
 * @param command           the launch command ({@code sun.java.command}: main class + args)
 * @param vmName            JVM name, e.g. "OpenJDK 64-Bit Server VM"
 * @param vmVendor          JVM vendor
 * @param vmVersion         JVM version
 * @param javaVersion       runtime version ({@code java.runtime.version})
 * @param javaHome          JAVA_HOME the target runs from
 * @param startTimeMillis   JVM start time, epoch millis
 * @param uptimeMillis      uptime in milliseconds
 * @param inputArguments    JVM input arguments (the {@code -X}/{@code -D} flags)
 * @param systemProperties  the target's system properties (sorted)
 */
public record JvmDetails(
        long pid,
        String command,
        String vmName,
        String vmVendor,
        String vmVersion,
        String javaVersion,
        String javaHome,
        long startTimeMillis,
        long uptimeMillis,
        List<String> inputArguments,
        Map<String, String> systemProperties
) {
}
