package com.jmonitor.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the jMonitor server.
 *
 * <p>Serves the REST/WebSocket API and (once built) the bundled React GUI from
 * the classpath {@code /static} directory.
 */
@EnableScheduling
@SpringBootApplication
public class JMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JMonitorApplication.class, args);
    }
}
