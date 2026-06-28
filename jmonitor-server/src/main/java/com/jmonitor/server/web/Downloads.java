package com.jmonitor.server.web;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

/** Builds file-download responses shared by the artifact controllers. */
final class Downloads {

    private Downloads() {
    }

    /**
     * Streams {@code file} as an attachment, or 404 if it no longer exists on
     * disk (the registry row may outlive the file).
     */
    static ResponseEntity<Resource> attachment(Path file, String fileName) {
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing: " + fileName);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }
}
