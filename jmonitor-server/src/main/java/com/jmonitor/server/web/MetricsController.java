package com.jmonitor.server.web;

import com.jmonitor.common.dto.MetricHistory;
import com.jmonitor.common.dto.MetricSnapshot;
import com.jmonitor.server.metrics.MetricArchive;
import com.jmonitor.server.metrics.MetricHistoryStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * REST access to metric data: the recent in-memory buffer (chart backfill for
 * the live view) and consolidated historical ranges from the RRD4J archive.
 */
@RestController
@RequestMapping("/api/processes/{pid}/metrics")
public class MetricsController {

    private final MetricHistoryStore history;
    private final MetricArchive archive;

    public MetricsController(MetricHistoryStore history, MetricArchive archive) {
        this.history = history;
        this.archive = archive;
    }

    @GetMapping("/recent")
    public List<MetricSnapshot> recent(@PathVariable long pid) {
        return history.recent(pid);
    }

    /**
     * Consolidated history for a time window. Defaults to the last hour.
     *
     * @param from    inclusive window start, epoch millis (optional)
     * @param to      inclusive window end, epoch millis (optional)
     * @param metrics comma-separated datasource names (optional; all if omitted)
     */
    @GetMapping("/history")
    public MetricHistory history(
            @PathVariable long pid,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(required = false) List<String> metrics) throws IOException {
        long now = System.currentTimeMillis();
        long toMillis = to != null ? to : now;
        long fromMillis = from != null ? from : toMillis - 3_600_000L;
        return archive.fetch(pid, fromMillis, toMillis,
                metrics == null ? List.of() : metrics);
    }
}
