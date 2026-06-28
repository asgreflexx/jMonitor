package com.jmonitor.server.web;

import com.jmonitor.common.dto.JvmDetails;
import com.jmonitor.common.dto.ProcessInfo;
import com.jmonitor.server.process.ProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    public JvmDetails details(@PathVariable long pid) {
        try {
            return service.details(pid);
        } catch (IOException e) {
            // The target could not be attached to (gone, not attachable, or denied).
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }
}
