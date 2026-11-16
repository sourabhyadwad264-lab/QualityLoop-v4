package com.qualityloop.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Builds the answer for a file download. Images and PDFs open in the browser, other files download.
final class FileResponses {

    private FileResponses() {}

    static ResponseEntity<Resource> of(String name, String contentType, Resource resource) {
        boolean inline = contentType.startsWith("image/") || contentType.equals("application/pdf");
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
