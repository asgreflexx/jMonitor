package com.jmonitor.server.support;

import java.nio.file.Path;

/** Path helpers shared by artifact stores (heap dumps, JFR recordings). */
public final class SafePaths {

    private SafePaths() {
    }

    /**
     * Resolves {@code fileName} inside {@code baseDir}, rejecting any name that
     * would escape the directory (path traversal). Used for download endpoints
     * where the file name comes from the artifact registry.
     */
    public static Path resolveWithin(Path baseDir, String fileName) {
        Path resolved = baseDir.resolve(fileName).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return resolved;
    }
}
