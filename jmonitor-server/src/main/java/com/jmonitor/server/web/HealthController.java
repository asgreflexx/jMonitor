package com.jmonitor.server.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal health/info endpoint used by the frontend shell to confirm
 * connectivity to the backend (Phase 0 end-to-end smoke check).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "app", "jMonitor",
                "version", "0.1.0-SNAPSHOT"
        );
    }
}
