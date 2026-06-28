package com.jmonitor.server.web;

import com.jmonitor.common.dto.JvmDetails;
import com.jmonitor.common.dto.ProcessInfo;
import com.jmonitor.server.process.ProcessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * REST endpoints for JVM discovery and details (Phase 1).
 */
@RestController
@RequestMapping("/api/processes")
public class ProcessController {

    private final ProcessService service;

    public ProcessController(ProcessService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProcessInfo> list() {
        return service.listProcesses();
    }

    @GetMapping("/{pid}")
    public JvmDetails details(@PathVariable long pid) throws IOException {
        // A target that can't be attached to (gone, not attachable, or denied)
        // throws IOException, mapped to 502 by ApiExceptionHandler.
        return service.details(pid);
    }
}
