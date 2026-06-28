package com.jmonitor.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Dynamically-attached agent that instruments methods of classes matching a
 * configured name prefix to measure per-method timing, exposing the results via
 * the {@link MethodProfilerMXBean} (Phase 6).
 *
 * <p>Loaded by the jMonitor server through {@code VirtualMachine.loadAgent}; the
 * prefix is passed as the agent argument string.
 */
public final class JMonitorAgent {

    private static final String OBJECT_NAME = "com.jmonitor:type=MethodProfiler";

    private JMonitorAgent() {
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        String prefix = args == null ? "" : args.trim();
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("A non-empty instrumentation prefix is required");
        }
        // Refuse to instrument core libraries — far too broad and unsafe. The
        // server (AgentService) validates the same way before attaching; this is
        // an independent backstop since the agent module is self-contained and
        // shares no code with the server.
        if (prefix.startsWith("java.") || prefix.startsWith("jdk.")
                || prefix.startsWith("sun.") || prefix.startsWith("com.sun.")) {
            throw new IllegalArgumentException("Refusing to instrument core package: " + prefix);
        }

        var server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(OBJECT_NAME);
        if (server.isRegistered(name)) {
            throw new IllegalStateException("jMonitor agent already loaded in this JVM");
        }

        AtomicInteger instrumentedClasses = new AtomicInteger();
        server.registerMBean(new MethodProfiler(prefix, instrumentedClasses), name);

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                // Never instrument the agent's own classes or ByteBuddy: doing so
                // would make TimingAdvice.exit -> MethodStats.record recurse
                // infinitely (StackOverflowError) if the prefix matched them.
                .ignore(nameStartsWith("com.jmonitor.agent").or(nameStartsWith("net.bytebuddy.")))
                .type(nameStartsWith(prefix))
                .transform((builder, typeDescription, classLoader, module, pd) -> builder
                        .visit(Advice.to(TimingAdvice.class)
                                .on(isMethod().and(not(isAbstract())).and(not(isNative()))
                                        .and(not(isConstructor())))))
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onTransformation(net.bytebuddy.description.type.TypeDescription typeDescription,
                                                 ClassLoader classLoader,
                                                 net.bytebuddy.utility.JavaModule module,
                                                 boolean loaded,
                                                 net.bytebuddy.dynamic.DynamicType dynamicType) {
                        instrumentedClasses.incrementAndGet();
                    }
                })
                .installOn(inst);
    }
}
