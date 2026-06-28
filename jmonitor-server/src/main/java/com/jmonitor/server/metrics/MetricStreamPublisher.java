package com.jmonitor.server.metrics;

import com.jmonitor.common.dto.MetricSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Samples every actively-subscribed JVM on a fixed interval and pushes each
 * {@link MetricSnapshot} to {@code /topic/metrics/{pid}}, also recording it in
 * the in-memory history buffer.
 */
@Component
public class MetricStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(MetricStreamPublisher.class);

    private final MetricSubscriptionRegistry registry;
    private final MetricSampler sampler;
    private final MetricHistoryStore history;
    private final MetricArchive archive;
    private final SimpMessagingTemplate messaging;

    public MetricStreamPublisher(MetricSubscriptionRegistry registry,
                                 MetricSampler sampler,
                                 MetricHistoryStore history,
                                 MetricArchive archive,
                                 SimpMessagingTemplate messaging) {
        this.registry = registry;
        this.sampler = sampler;
        this.history = history;
        this.archive = archive;
        this.messaging = messaging;
    }

    @Scheduled(fixedRateString = "${jmonitor.sample-interval-ms:1000}")
    public void publish() {
        for (long pid : registry.activePids()) {
            try {
                MetricSnapshot snapshot = sampler.sample(pid);
                history.add(snapshot);
                archive.record(snapshot);
                messaging.convertAndSend("/topic/metrics/" + pid, snapshot);
            } catch (IOException e) {
                // Target likely exited or became unreachable; drop its history.
                log.debug("Sampling pid {} failed: {}", pid, e.getMessage());
                history.evict(pid);
            }
        }
    }
}
