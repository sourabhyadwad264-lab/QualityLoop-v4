package com.qualityloop.controller;

import com.qualityloop.entity.Attachment;
import com.qualityloop.service.AttachmentService;
import com.qualityloop.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

// Photos and PDFs attached to a defect. Deleting is limited to Quality Head and Admin by the URL rule in SecurityConfig.
@RestController
@RequestMapping("/api/defects/{defectId}/attachments")
public class AttachmentController {

    private final AttachmentService service;
    private final FileStorageService storage;

    public AttachmentController(AttachmentService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping
    public List<Attachment> list(@PathVariable Long defectId) { return service.list(defectId); }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Attachment upload(@PathVariable Long defectId,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam(value = "shared", defaultValue = "false") boolean shared) {
        return service.add(defectId, file, shared);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long defectId, @PathVariable Long id) {
        Attachment a = service.get(defectId, id);
        return FileResponses.of(a.getOriginalName(), a.getContentType(), storage.load(a.getStoredName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long defectId, @PathVariable Long id) { service.delete(defectId, id); }
}
