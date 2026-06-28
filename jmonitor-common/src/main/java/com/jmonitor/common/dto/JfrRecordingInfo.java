package com.jmonitor.common.dto;

/**
 * Metadata for a completed JFR recording saved on the server (Phase 5).
 *
 * @param id            registry id
 * @param pid           the process the recording was taken from
 * @param fileName      the {@code .jfr} file name under the server's recordings dir
 * @param sizeBytes     file size in bytes
 * @param createdMillis capture (stop) time, epoch millis
 * @param profile       the JFR configuration used ("default" or "profile")
 */
public record JfrRecordingInfo(
        long id,
        long pid,
        String fileName,
        long sizeBytes,
        long createdMillis,
        String profile
) {
}
