package com.jmonitor.common.dto;

/**
 * Metadata for a captured heap dump file (Phase 4), tracked in the H2 registry.
 *
 * @param id           registry id
 * @param pid          the process the dump was taken from
 * @param fileName     the dump file name (under the server's dump directory)
 * @param sizeBytes    file size in bytes
 * @param createdMillis capture time, epoch millis
 * @param live         whether only live (reachable) objects were dumped
 */
public record HeapDumpInfo(
        long id,
        long pid,
        String fileName,
        long sizeBytes,
        long createdMillis,
        boolean live
) {
}
