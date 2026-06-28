package com.jmonitor.common.dto;

import java.util.List;

/**
 * A node in an aggregated flame graph (Phase 5): a frame and the number of
 * stack samples that passed through it, with child frames below it.
 *
 * @param name     frame label (e.g. {@code "java.lang.Thread.run"}) or "all" at the root
 * @param value    total samples through this frame (sum of own + descendants)
 * @param children child frames, largest first
 */
public record FlameNode(
        String name,
        long value,
        List<FlameNode> children
) {
}
