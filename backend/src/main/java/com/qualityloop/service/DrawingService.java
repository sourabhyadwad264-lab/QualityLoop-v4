package com.qualityloop.service;

import com.qualityloop.entity.Drawing;
import com.qualityloop.entity.DrawingRevision;
import com.qualityloop.exception.BadRequestException;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.DrawingRepository;
import com.qualityloop.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

// Drawing register with revision control: a drawing is never overwritten or deleted.
// A change is saved as the next revision, and every older revision stays available.
@Service
@Transactional
public class DrawingService {

    private final DrawingRepository drawings;
    private final FileStorageService storage;

    public DrawingService(DrawingRepository drawings, FileStorageService storage) {
        this.drawings = drawings;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<Drawing> list(String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return drawings.findAllByOrderByPartNumberAsc().stream()
                .filter(d -> needle.isEmpty() || d.getPartNumber().toLowerCase().contains(needle)
                        || d.getTitle().toLowerCase().contains(needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public Drawing get(Long id) {
        return drawings.findById(id).orElseThrow(() -> new NotFoundException("Drawing " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Drawing> forPart(String partNumber) {
        if (partNumber == null || partNumber.isBlank()) return Optional.empty();
        return drawings.findByPartNumberIgnoreCase(partNumber.trim());
    }

    public Drawing create(String partNumber, String title, String changeNote, MultipartFile file) {
        String pn = partNumber == null ? "" : partNumber.trim();
        if (pn.isEmpty()) throw new BadRequestException("Part number is required");
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required");
        if (drawings.existsByPartNumberIgnoreCase(pn))
            throw new BusinessRuleException("A drawing for part " + pn + " already exists. Upload a new revision instead");

        Drawing d = new Drawing();
        d.setPartNumber(pn);
        d.setTitle(title.trim());
        d.setCreatedBy(CurrentUser.name());
        d.getRevisions().add(newRevision(d, 1, file, changeNote));
        return drawings.save(d);
    }

    public Drawing addRevision(Long id, String changeNote, MultipartFile file) {
        Drawing d = get(id);
        int next = d.getRevisions().stream().mapToInt(DrawingRevision::getRevisionNumber).max().orElse(0) + 1;
        d.getRevisions().add(0, newRevision(d, next, file, changeNote));
        return drawings.saveAndFlush(d);
    }

    @Transactional(readOnly = true)
    public DrawingRevision revision(Long drawingId, Long revisionId) {
        return get(drawingId).getRevisions().stream()
                .filter(r -> r.getId().equals(revisionId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Revision " + revisionId + " not found"));
    }

    private DrawingRevision newRevision(Drawing d, int number, MultipartFile file, String note) {
        FileStorageService.StoredFile f = storage.save(file, FileStorageService.DRAWING);
        DrawingRevision r = new DrawingRevision();
        r.setDrawing(d);
        r.setRevisionNumber(number);
        r.setOriginalName(f.originalName());
        r.setStoredName(f.storedName());
        r.setContentType(f.contentType());
        r.setSizeBytes(f.size());
        String n = note == null ? "" : note.trim();
        r.setChangeNote(n.length() > 500 ? n.substring(0, 500) : n);
        r.setUploadedBy(CurrentUser.name());
        return r;
    }
}
