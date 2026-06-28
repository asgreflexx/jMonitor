package com.jmonitor.agent;

/**
 * MBean the agent registers in the target JVM so the jMonitor server can read
 * method-timing hotspots over the existing JMX connection (no extra socket).
 *
 * <p>Registered under {@code com.jmonitor:type=MethodProfiler}.
 */
public interface MethodProfilerMXBean {

    /** The package/class-name prefix being instrumented. */
    String getInstrumentedPrefix();

    /** Number of classes transformed so far. */
    int getInstrumentedClassCount();

    /**
     * Top methods by total time, one per line as
     * {@code method\tcalls\ttotalNanos}.
     */
    String getHotspots();

    /** Clears all accumulated counters. */
    void reset();
}
