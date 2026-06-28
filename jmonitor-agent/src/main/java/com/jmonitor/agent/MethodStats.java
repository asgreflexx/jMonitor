package com.jmonitor.agent;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Process-wide accumulator of per-method call counts and total wall-clock time,
 * populated by {@link TimingAdvice} inlined into instrumented methods.
 *
 * <p>Lives in the agent jar (system class loader) so instrumented application
 * classes can reach it. Thread-safe and allocation-light on the hot path.
 */
public final class MethodStats {

    /** Counters for one method signature. */
    static final class Counter {
        final LongAdder calls = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
    }

    private static final ConcurrentHashMap<String, Counter> COUNTERS = new ConcurrentHashMap<>();

    private MethodStats() {
    }

    /** Records one completed invocation. Called from instrumented method exits. */
    public static void record(String method, long nanos) {
        Counter c = COUNTERS.computeIfAbsent(method, k -> new Counter());
        c.calls.increment();
        c.totalNanos.add(nanos);
    }

    public static void reset() {
        COUNTERS.clear();
    }

    /**
     * Serialises the top {@code limit} methods by total time as one row per line:
     * {@code method\tcalls\ttotalNanos}. A compact, dependency-free wire format
     * the server parses back into structured hotspots.
     */
    public static String dump(int limit) {
        StringBuilder sb = new StringBuilder();
        COUNTERS.entrySet().stream()
                .sorted(Comparator.comparingLong(
                        (Map.Entry<String, Counter> e) -> e.getValue().totalNanos.sum()).reversed())
                .limit(limit)
                .forEach(e -> sb.append(e.getKey()).append('\t')
                        .append(e.getValue().calls.sum()).append('\t')
                        .append(e.getValue().totalNanos.sum()).append('\n'));
        return sb.toString();
    }
}
