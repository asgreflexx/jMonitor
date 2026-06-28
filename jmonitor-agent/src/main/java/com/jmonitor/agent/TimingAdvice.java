package com.jmonitor.agent;

import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice inlined into every instrumented method to measure its
 * wall-clock duration and feed {@link MethodStats}.
 *
 * <p>The enter/exit bodies are copied into the target method bytecode, so they
 * must only reference types reachable from the instrumented class loader
 * (MethodStats is on the system class path).
 */
public final class TimingAdvice {

    private TimingAdvice() {
    }

    @Advice.OnMethodEnter
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Origin("#t.#m#d") String method, @Advice.Enter long start) {
        // "#t.#m#d" includes the method descriptor so overloaded methods are
        // counted separately rather than merged.
        MethodStats.record(method, System.nanoTime() - start);
    }
}
