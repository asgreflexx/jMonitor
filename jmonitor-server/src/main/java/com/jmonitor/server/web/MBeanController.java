package com.jmonitor.server.web;

import com.jmonitor.common.dto.MBeanDetails;
import com.jmonitor.server.mbean.MBeanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the MBean browser (Phase 4). MBean object names are passed
 * as a {@code name} query parameter (they contain reserved URL characters).
 */
@RestController
@RequestMapping("/api/processes/{pid}/mbeans")
public class MBeanController {

    private final MBeanService mbeans;

    public MBeanController(MBeanService mbeans) {
        this.mbeans = mbeans;
    }

    @GetMapping
    public List<String> list(@PathVariable long pid) {
        return wrap(() -> mbeans.listNames(pid));
    }

    @GetMapping("/details")
    public MBeanDetails details(@PathVariable long pid, @RequestParam String name) {
        return wrap(() -> mbeans.details(pid, name));
    }

    @PostMapping("/attribute")
    public void setAttribute(@PathVariable long pid,
                             @RequestParam String name,
                             @RequestParam String attribute,
                             @RequestParam String value) {
        wrap(() -> {
            mbeans.setAttribute(pid, name, attribute, value);
            return null;
        });
    }

    @PostMapping("/invoke")
    public Map<String, String> invoke(@PathVariable long pid,
                                      @RequestParam String name,
                                      @RequestParam String operation) {
        return Map.of("result", wrap(() -> mbeans.invokeNoArg(pid, name, operation)));
    }

    private static <T> T wrap(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
