package com.jmonitor.server.web;

import com.jmonitor.common.dto.MBeanDetails;
import com.jmonitor.server.mbean.MBeanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the MBean browser (Phase 4). MBean object names are passed
 * as a {@code name} query parameter (they contain reserved URL characters).
 *
 * <p>{@link IOException}s (target unreachable) map to 502 and
 * {@link IllegalArgumentException}s (bad input) to 400 via
 * {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/processes/{pid}/mbeans")
public class MBeanController {

    private final MBeanService mbeans;

    public MBeanController(MBeanService mbeans) {
        this.mbeans = mbeans;
    }

    @GetMapping
    public List<String> list(@PathVariable long pid) throws IOException {
        return mbeans.listNames(pid);
    }

    @GetMapping("/details")
    public MBeanDetails details(@PathVariable long pid, @RequestParam String name) throws IOException {
        return mbeans.details(pid, name);
    }

    @PostMapping("/attribute")
    public void setAttribute(@PathVariable long pid,
                             @RequestParam String name,
                             @RequestParam String attribute,
                             @RequestParam String value) throws IOException {
        mbeans.setAttribute(pid, name, attribute, value);
    }

    @PostMapping("/invoke")
    public Map<String, String> invoke(@PathVariable long pid,
                                      @RequestParam String name,
                                      @RequestParam String operation) throws IOException {
        return Map.of("result", mbeans.invokeNoArg(pid, name, operation));
    }
}
