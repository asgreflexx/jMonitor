package com.jmonitor.server.metrics;

import com.jmonitor.common.dto.MetricSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory ring buffer of recent {@link MetricSnapshot}s per pid.
 *
 * <p>Used to backfill charts when a client first opens a process, so the graphs
 * aren't empty. Persistent long-term history arrives in Phase 3 (RRD4J).
 */
@Component
public class MetricHistoryStore {

    private final int capacity;
    private final Map<Long, Deque<MetricSnapshot>> byPid = new ConcurrentHashMap<>();

    public MetricHistoryStore(@Value("${jmonitor.history-size:300}") int capacity) {
        this.capacity = capacity;
    }

    public void add(MetricSnapshot snapshot) {
        Deque<MetricSnapshot> queue =
                byPid.computeIfAbsent(snapshot.pid(), k -> new ConcurrentLinkedDeque<>());
        queue.addLast(snapshot);
        while (queue.size() > capacity) {
            queue.pollFirst();
        }
    }

    public List<MetricSnapshot> recent(long pid) {
        Deque<MetricSnapshot> queue = byPid.get(pid);
        return queue == null ? List.of() : new ArrayList<>(queue);
    }

    public void evict(long pid) {
        byPid.remove(pid);
    }
}
