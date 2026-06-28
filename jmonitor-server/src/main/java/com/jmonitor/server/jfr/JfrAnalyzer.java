package com.jmonitor.server.jfr;

import com.jmonitor.common.dto.FlameNode;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a {@code .jfr} recording and aggregates its CPU execution samples into
 * a flame-graph tree (Phase 5).
 *
 * <p>Each {@code jdk.ExecutionSample} / {@code jdk.NativeMethodSample} stack is
 * folded into the tree from the outermost frame inwards, incrementing a sample
 * count at every frame.
 */
@Service
public class JfrAnalyzer {

    private static final java.util.Set<String> SAMPLE_EVENTS =
            java.util.Set.of("jdk.ExecutionSample", "jdk.NativeMethodSample");

    public FlameNode flameGraph(Path jfrFile) throws IOException {
        Node root = new Node("all");
        try (RecordingFile rf = new RecordingFile(jfrFile)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                if (!SAMPLE_EVENTS.contains(event.getEventType().getName())) {
                    continue;
                }
                RecordedStackTrace stack = event.getStackTrace();
                if (stack == null) {
                    continue;
                }
                List<RecordedFrame> frames = stack.getFrames();
                root.value++;
                Node current = root;
                // Frames are innermost-first; walk outermost-first to build the tree.
                for (int i = frames.size() - 1; i >= 0; i--) {
                    current = current.child(frameName(frames.get(i)));
                    current.value++;
                }
            }
        }
        return root.toDto();
    }

    private static String frameName(RecordedFrame frame) {
        RecordedMethod method = frame.getMethod();
        if (method == null) {
            return "<unknown>";
        }
        String type = method.getType() == null ? "" : method.getType().getName();
        return type.isEmpty() ? method.getName() : type + "." + method.getName();
    }

    /** Mutable tree node used during aggregation. */
    private static final class Node {
        final String name;
        long value;
        final Map<String, Node> children = new LinkedHashMap<>();

        Node(String name) {
            this.name = name;
        }

        Node child(String childName) {
            return children.computeIfAbsent(childName, Node::new);
        }

        FlameNode toDto() {
            List<FlameNode> kids = new ArrayList<>(children.size());
            for (Node c : children.values()) {
                kids.add(c.toDto());
            }
            kids.sort(Comparator.comparingLong(FlameNode::value).reversed());
            return new FlameNode(name, value, kids);
        }
    }
}
