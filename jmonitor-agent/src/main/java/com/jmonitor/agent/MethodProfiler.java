package com.jmonitor.agent;

import java.util.concurrent.atomic.AtomicInteger;

/** {@link MethodProfilerMXBean} implementation backed by {@link MethodStats}. */
public final class MethodProfiler implements MethodProfilerMXBean {

    /** Top-N cap on the hotspot list returned over JMX. */
    private static final int HOTSPOT_LIMIT = 200;

    private final String prefix;
    private final AtomicInteger instrumentedClasses;

    public MethodProfiler(String prefix, AtomicInteger instrumentedClasses) {
        this.prefix = prefix;
        this.instrumentedClasses = instrumentedClasses;
    }

    @Override
    public String getInstrumentedPrefix() {
        return prefix;
    }

    @Override
    public int getInstrumentedClassCount() {
        return instrumentedClasses.get();
    }

    @Override
    public String getHotspots() {
        return MethodStats.dump(HOTSPOT_LIMIT);
    }

    @Override
    public void reset() {
        MethodStats.reset();
    }
}
