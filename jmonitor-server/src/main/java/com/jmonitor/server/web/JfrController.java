package com.jmonitor.server.web;

import com.jmonitor.common.dto.FlameNode;
import com.jmonitor.common.dto.JfrRecordingInfo;
import com.jmonitor.common.dto.JfrStatus;
import com.jmonitor.server.jfr.JfrAnalyzer;
import com.jmonitor.server.jfr.JfrRegistry;
import com.jmonitor.server.jfr.JfrService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * REST endpoints for JFR profiling (Phase 5): start/stop recordings, list saved
 * recordings, render flame graphs and download {@code .jfr} files.
 */
@RestController
public class JfrController {

    private final JfrService jfr;
    private final JfrRegistry registry;
    private final JfrAnalyzer analyzer;

    public JfrController(JfrService jfr, JfrRegistry registry, JfrAnalyzer analyzer) {
        this.jfr = jfr;
        this.registry = registry;
        this.analyzer = analyzer;
    }

    @GetMapping("/api/processes/{pid}/jfr/status")
    public JfrStatus status(@PathVariable long pid) {
        return jfr.status(pid);
    }

    @PostMapping("/api/processes/{pid}/jfr/start")
    public JfrStatus start(@PathVariable long pid,
                           @RequestParam(defaultValue = "profile") String profile) throws IOException {
        jfr.start(pid, profile);
        return new JfrStatus(true, profile);
    }

    @PostMapping("/api/processes/{pid}/jfr/stop")
    public JfrRecordingInfo stop(@PathVariable long pid) throws IOException {
        return jfr.stop(pid);
    }

    @GetMapping("/api/processes/{pid}/jfr/recordings")
    public List<JfrRecordingInfo> recordings(@PathVariable long pid) {
        return registry.listForPid(pid);
    }

    @GetMapping("/api/jfr/recordings/{id}/flamegraph")
    public FlameNode flameGraph(@PathVariable long id) throws IOException {
        JfrRecordingInfo info = recording(id);
        Path file = jfr.resolveRecordingFile(info.fileName());
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording file missing");
        }
        return analyzer.flameGraph(file);
    }

    @GetMapping("/api/jfr/recordings/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        JfrRecordingInfo info = recording(id);
        return Downloads.attachment(jfr.resolveRecordingFile(info.fileName()), info.fileName());
    }

    private JfrRecordingInfo recording(long id) {
        return registry.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown recording " + id));
    }
}
