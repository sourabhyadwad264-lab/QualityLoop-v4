package com.qualityloop.controller;

import com.qualityloop.entity.Drawing;
import com.qualityloop.entity.DrawingRevision;
import com.qualityloop.service.DrawingService;
import com.qualityloop.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

// Everyone logged in can view drawings. Only a Quality Head or Admin can add a drawing or a new revision.
@RestController
@RequestMapping("/api/drawings")
public class DrawingController {

    private final DrawingService service;
    private final FileStorageService storage;

    public DrawingController(DrawingService service, FileStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping
    public List<Drawing> list(@RequestParam(required = false) String q) { return service.list(q); }

    // 200 with the drawing, or 204 (empty) when the part has no drawing yet
    @GetMapping("/by-part")
    public ResponseEntity<Drawing> byPart(@RequestParam(required = false) String partNumber) {
        return service.forPart(partNumber).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_HEAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public Drawing create(@RequestParam(required = false) String partNumber,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) String changeNote,
                          @RequestParam(value = "file", required = false) MultipartFile file) {
        return service.create(partNumber, title, changeNote, file);
    }

    @PostMapping(value = "/{id}/revisions", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_HEAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public Drawing addRevision(@PathVariable Long id,
                               @RequestParam(required = false) String changeNote,
                               @RequestParam(value = "file", required = false) MultipartFile file) {
        return service.addRevision(id, changeNote, file);
    }

    @GetMapping("/{id}/revisions/{revisionId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, @PathVariable Long revisionId) {
        DrawingRevision r = service.revision(id, revisionId);
        return FileResponses.of(r.getOriginalName(), r.getContentType(), storage.load(r.getStoredName()));
    }
}
