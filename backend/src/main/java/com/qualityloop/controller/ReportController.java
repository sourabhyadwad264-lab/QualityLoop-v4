package com.qualityloop.controller;

import com.qualityloop.dto.EmailRequest;
import com.qualityloop.dto.ReportSummary;
import com.qualityloop.dto.ReportView;
import com.qualityloop.dto.ShareResponse;
import com.qualityloop.entity.Attachment;
import com.qualityloop.service.AttachmentService;
import com.qualityloop.service.FileStorageService;
import com.qualityloop.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {

    private final ReportService service;
    private final AttachmentService attachments;
    private final FileStorageService storage;

    public ReportController(ReportService service, AttachmentService attachments, FileStorageService storage) {
        this.service = service;
        this.attachments = attachments;
        this.storage = storage;
    }

    @GetMapping("/api/reports/summary")
    public ReportSummary summary() { return service.summary(); }

    @PostMapping("/api/defects/{id}/share")
    public ShareResponse share(@PathVariable Long id) { return service.share(id); }

    @PostMapping("/api/defects/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void email(@PathVariable Long id, @Valid @RequestBody EmailRequest r) {
        service.emailReport(id, r.to(), r.message());
    }

    // Open to anyone who has the secret link
    @GetMapping("/api/public/reports/{token}")
    public ReportView publicReport(@PathVariable String token) { return service.publicReport(token); }

    // A photo that the team marked "show to customer", opened with the same secret link
    @GetMapping("/api/public/reports/{token}/files/{id}")
    public ResponseEntity<Resource> publicFile(@PathVariable String token, @PathVariable Long id) {
        Attachment a = attachments.publicGet(token, id);
        return FileResponses.of(a.getOriginalName(), a.getContentType(), storage.load(a.getStoredName()));
    }
}
