package com.jmonitor.common.dto;

import java.util.List;

/**
 * A full thread dump of a JVM (Phase 4).
 *
 * @param pid          the process the dump was taken from
 * @param epochMillis  capture time, epoch millis
 * @param threads      all live threads with their stacks
 * @param deadlockedIds ids of threads involved in a detected deadlock (may be empty)
 */
public record ThreadDump(
        long pid,
        long epochMillis,
        List<ThreadInfoDto> threads,
        List<Long> deadlockedIds
) {
}
