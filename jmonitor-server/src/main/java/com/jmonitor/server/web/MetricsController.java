package com.jmonitor.server.web;

import com.jmonitor.common.dto.MetricHistory;
import com.jmonitor.common.dto.MetricSnapshot;
import com.jmonitor.server.metrics.MetricArchive;
import com.jmonitor.server.metrics.MetricHistoryStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    /** Exports consolidated history for a window as a CSV download (Phase 7). */
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv(
            @PathVariable long pid,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) throws IOException {
        long now = System.currentTimeMillis();
        long toMillis = to != null ? to : now;
        long fromMillis = from != null ? from : toMillis - 3_600_000L;
        MetricHistory h = archive.fetch(pid, fromMillis, toMillis, List.of());

        List<String> columns = new ArrayList<>(h.series().keySet());
        StringBuilder csv = new StringBuilder("epochMillis");
        for (String c : columns) {
            csv.append(',').append(c);
        }
        csv.append('\n');
        for (int i = 0; i < h.timestamps().length; i++) {
            csv.append(h.timestamps()[i]);
            for (String c : columns) {
                double[] values = h.series().get(c);
                double v = i < values.length ? values[i] : Double.NaN;
                // toPlainString avoids scientific notation (e.g. 1.7E8) that
                // trips up spreadsheets for large byte counts.
                csv.append(',').append(Double.isNaN(v) ? "" : BigDecimal.valueOf(v).toPlainString());
            }
            csv.append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"jmonitor-" + pid + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
