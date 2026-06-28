package com.jmonitor.server.alert;

import com.jmonitor.common.dto.Alert;
import com.jmonitor.common.dto.MetricSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a {@link MetricSnapshot} against configurable thresholds and reports
 * active alerts (Phase 7). Thresholds are percentages, configurable under
 * {@code jmonitor.alerts.*}.
 */
@Service
public class AlertService {

    private final double heapWarn;
    private final double heapCritical;
    private final double cpuWarn;
    private final double cpuCritical;

    public AlertService(
            @Value("${jmonitor.alerts.heap-warn-percent:80}") double heapWarn,
            @Value("${jmonitor.alerts.heap-critical-percent:90}") double heapCritical,
            @Value("${jmonitor.alerts.cpu-warn-percent:85}") double cpuWarn,
            @Value("${jmonitor.alerts.cpu-critical-percent:95}") double cpuCritical) {
        this.heapWarn = heapWarn;
        this.heapCritical = heapCritical;
        this.cpuWarn = cpuWarn;
        this.cpuCritical = cpuCritical;
    }

    public List<Alert> evaluate(MetricSnapshot s) {
        List<Alert> alerts = new ArrayList<>();
        if (s == null) {
            return alerts;
        }

        if (s.heapMax() > 0) {
            double heapPct = 100.0 * s.heapUsed() / s.heapMax();
            addIfBreached(alerts, "heap", heapPct, heapWarn, heapCritical, "Heap usage");
        }

        if (s.processCpuLoad() >= 0) {
            double cpuPct = 100.0 * s.processCpuLoad();
            addIfBreached(alerts, "cpu", cpuPct, cpuWarn, cpuCritical, "Process CPU");
        }

        return alerts;
    }

    private static void addIfBreached(List<Alert> alerts, String metric, double value,
                                      double warn, double critical, String label) {
        if (value >= critical) {
            alerts.add(new Alert("CRITICAL", metric,
                    "%s %.0f%% ≥ %.0f%%".formatted(label, value, critical), value, critical));
        } else if (value >= warn) {
            alerts.add(new Alert("WARNING", metric,
                    "%s %.0f%% ≥ %.0f%%".formatted(label, value, warn), value, warn));
        }
    }
}
