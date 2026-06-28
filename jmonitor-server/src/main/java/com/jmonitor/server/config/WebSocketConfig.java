package com.jmonitor.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup for live metric streaming (Phase 2).
 *
 * <p>Clients connect to {@code /ws} and subscribe to {@code /topic/metrics/{pid}}.
 * A simple in-memory broker fans messages out; no external broker is required.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket (no SockJS). allowedOriginPatterns covers the Vite dev origin.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
