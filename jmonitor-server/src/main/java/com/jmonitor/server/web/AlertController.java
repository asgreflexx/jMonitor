package com.jmonitor.server.web;

import com.jmonitor.common.dto.Alert;
import com.jmonitor.common.dto.MetricSnapshot;
import com.jmonitor.server.alert.AlertService;
import com.jmonitor.server.metrics.MetricHistoryStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reports active threshold alerts for a process, evaluated against the most
 * recent sampled metrics (Phase 7).
 */
@RestController
public class AlertController {

    private final MetricHistoryStore history;
    private final AlertService alerts;

    public AlertController(MetricHistoryStore history, AlertService alerts) {
        this.history = history;
        this.alerts = alerts;
    }

    @GetMapping("/api/processes/{pid}/alerts")
    public List<Alert> alerts(@PathVariable long pid) {
        List<MetricSnapshot> recent = history.recent(pid);
        MetricSnapshot latest = recent.isEmpty() ? null : recent.get(recent.size() - 1);
        return alerts.evaluate(latest);
    }
}
