package com.jmonitor.server.metrics;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks which JVM pids currently have at least one live STOMP subscriber on
 * {@code /topic/metrics/{pid}}.
 *
 * <p>The publisher only samples pids reported by {@link #activePids()}, so idle
 * JVMs are never polled. Subscriptions are reference-counted and cleaned up on
 * unsubscribe and on session disconnect.
 */
@Component
public class MetricSubscriptionRegistry {

    private static final Pattern TOPIC = Pattern.compile("/topic/metrics/(\\d+)");

    /** sessionId -> (subscriptionId -> pid) */
    private final Map<String, Map<String, Long>> bySession = new ConcurrentHashMap<>();
    /** pid -> live subscriber count */
    private final Map<Long, AtomicInteger> counts = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(event.getMessage());
        Long pid = pidOf(h.getDestination());
        if (pid == null) {
            return;
        }
        bySession.computeIfAbsent(h.getSessionId(), k -> new ConcurrentHashMap<>())
                .put(h.getSubscriptionId(), pid);
        counts.computeIfAbsent(pid, k -> new AtomicInteger()).incrementAndGet();
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Long> subs = bySession.get(h.getSessionId());
        if (subs != null) {
            decrement(subs.remove(h.getSubscriptionId()));
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Map<String, Long> subs = bySession.remove(event.getSessionId());
        if (subs != null) {
            subs.values().forEach(this::decrement);
        }
    }

    public Set<Long> activePids() {
        return new HashSet<>(counts.keySet());
    }

    private void decrement(Long pid) {
        if (pid == null) {
            return;
        }
        AtomicInteger count = counts.get(pid);
        if (count != null && count.decrementAndGet() <= 0) {
            counts.remove(pid);
        }
    }

    private static Long pidOf(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher m = TOPIC.matcher(destination);
        return m.matches() ? Long.parseLong(m.group(1)) : null;
    }
}
