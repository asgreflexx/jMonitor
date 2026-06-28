package com.jmonitor.server.web;

import com.jmonitor.common.dto.HeapDumpInfo;
import com.jmonitor.common.dto.HeapHistogram;
import com.jmonitor.common.dto.ThreadDump;
import com.jmonitor.server.diagnostics.HeapDumpRegistry;
import com.jmonitor.server.diagnostics.HeapService;
import com.jmonitor.server.diagnostics.ThreadDumpService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * REST endpoints for thread dumps and heap diagnostics (Phase 4).
 *
 * <p>{@link IOException}s from the target connection propagate to
 * {@link ApiExceptionHandler}, which maps them to 502.
 */
@RestController
public class DiagnosticsController {

    private final ThreadDumpService threadDumps;
    private final HeapService heap;
    private final HeapDumpRegistry registry;

    public DiagnosticsController(ThreadDumpService threadDumps, HeapService heap,
                                 HeapDumpRegistry registry) {
        this.threadDumps = threadDumps;
        this.heap = heap;
        this.registry = registry;
    }

    @GetMapping("/api/processes/{pid}/threaddump")
    public ThreadDump threadDump(@PathVariable long pid) throws IOException {
        return threadDumps.capture(pid);
    }

    @GetMapping("/api/processes/{pid}/heap/histogram")
    public HeapHistogram histogram(@PathVariable long pid) throws IOException {
        return heap.histogram(pid);
    }

    @PostMapping("/api/processes/{pid}/heap/dump")
    public HeapDumpInfo dumpHeap(@PathVariable long pid,
                                 @RequestParam(defaultValue = "true") boolean live) throws IOException {
        return heap.dumpHeap(pid, live);
    }

    @GetMapping("/api/processes/{pid}/heap/dumps")
    public List<HeapDumpInfo> dumps(@PathVariable long pid) {
        return registry.listForPid(pid);
    }

    @GetMapping("/api/heap/dumps/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        HeapDumpInfo info = registry.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown dump " + id));
        Path file = heap.resolveDumpFile(info.fileName());
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dump file missing: " + info.fileName());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.fileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }
}
