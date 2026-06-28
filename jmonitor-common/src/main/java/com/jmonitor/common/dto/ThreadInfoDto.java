package com.jmonitor.common.dto;

import java.util.List;

/**
 * One thread's state and stack at the moment a thread dump was taken (Phase 4).
 *
 * @param id            the thread id
 * @param name          the thread name
 * @param state         the {@link Thread.State} name, e.g. "RUNNABLE", "BLOCKED"
 * @param stackTrace    formatted stack frames, outermost (top) first
 * @param lockName      the lock the thread is blocked/waiting on, or null
 * @param lockOwnerId   id of the thread holding that lock, or -1
 * @param lockOwnerName name of the thread holding that lock, or null
 * @param blockedCount  total times the thread was blocked
 * @param waitedCount   total times the thread waited
 * @param inNative      whether the thread is executing native code
 * @param suspended     whether the thread is suspended
 * @param deadlocked    whether this thread is part of a detected deadlock
 */
public record ThreadInfoDto(
        long id,
        String name,
        String state,
        List<String> stackTrace,
        String lockName,
        long lockOwnerId,
        String lockOwnerName,
        long blockedCount,
        long waitedCount,
        boolean inNative,
        boolean suspended,
        boolean deadlocked
) {
}
