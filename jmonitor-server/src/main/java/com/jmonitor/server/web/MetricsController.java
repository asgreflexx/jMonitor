package com.jmonitor.server.web;

import com.jmonitor.common.dto.MetricSnapshot;
import com.jmonitor.server.metrics.MetricHistoryStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST access to recent in-memory metric history, used by the GUI to backfill
 * charts before the live WebSocket stream takes over.
 */
@RestController
@RequestMapping("/api/processes/{pid}/metrics")
public class MetricsController {

    private final MetricHistoryStore history;

    public MetricsController(MetricHistoryStore history) {
        this.history = history;
    }

    @GetMapping("/recent")
    public List<MetricSnapshot> recent(@PathVariable long pid) {
        return history.recent(pid);
    }
}
