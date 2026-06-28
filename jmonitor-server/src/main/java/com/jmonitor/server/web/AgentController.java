package com.jmonitor.server.web;

import com.jmonitor.common.dto.AgentStatus;
import com.jmonitor.common.dto.MethodHotspot;
import com.jmonitor.server.agent.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * REST endpoints for the instrumentation agent (Phase 6).
 *
 * <p>{@link IOException} (target unreachable / attach failure) maps to 502 and
 * {@link IllegalArgumentException} (bad prefix, already loaded, not loaded) to
 * 400 via {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/processes/{pid}/agent")
public class AgentController {

    private final AgentService agent;

    public AgentController(AgentService agent) {
        this.agent = agent;
    }

    @GetMapping("/status")
    public AgentStatus status(@PathVariable long pid) throws IOException {
        return agent.status(pid);
    }

    @PostMapping("/load")
    public AgentStatus load(@PathVariable long pid, @RequestParam String prefix) throws IOException {
        agent.load(pid, prefix);
        return agent.status(pid);
    }

    @GetMapping("/hotspots")
    public List<MethodHotspot> hotspots(@PathVariable long pid) throws IOException {
        return agent.hotspots(pid);
    }

    @PostMapping("/reset")
    public void reset(@PathVariable long pid) throws IOException {
        agent.reset(pid);
    }
}
