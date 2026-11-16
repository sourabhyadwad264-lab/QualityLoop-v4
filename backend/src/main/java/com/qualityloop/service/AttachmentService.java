package com.qualityloop.service;

import com.qualityloop.entity.Attachment;
import com.qualityloop.entity.AuditEntry;
import com.qualityloop.entity.Defect;
import com.qualityloop.entity.DefectStatus;
import com.qualityloop.exception.BusinessRuleException;
import com.qualityloop.exception.NotFoundException;
import com.qualityloop.repository.AttachmentRepository;
import com.qualityloop.repository.AuditEntryRepository;
import com.qualityloop.repository.DefectRepository;
import com.qualityloop.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AttachmentService {

    private static final int MAX_FILES_PER_DEFECT = 10;

    private final AttachmentRepository attachments;
    private final DefectRepository defects;
    private final AuditEntryRepository audits;
    private final FileStorageService storage;

    public AttachmentService(AttachmentRepository attachments, DefectRepository defects,
                             AuditEntryRepository audits, FileStorageService storage) {
        this.attachments = attachments;
        this.defects = defects;
        this.audits = audits;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<Attachment> list(Long defectId) {
        requireDefect(defectId);
        return attachments.findByDefectIdOrderByUploadedAtDesc(defectId);
    }

    public Attachment add(Long defectId, MultipartFile file, boolean shared) {
        Defect d = requireDefect(defectId);
        if (d.getStatus() == DefectStatus.CLOSED)
            throw new BusinessRuleException("Defect " + defectId + " is closed and cannot be changed");
        if (attachments.countByDefectId(defectId) >= MAX_FILES_PER_DEFECT)
            throw new BusinessRuleException("A defect can have at most " + MAX_FILES_PER_DEFECT + " files");

        FileStorageService.StoredFile f = storage.save(file, FileStorageService.EVIDENCE);
        Attachment a = new Attachment();
        a.setDefectId(defectId);
        a.setOriginalName(f.originalName());
        a.setStoredName(f.storedName());
        a.setContentType(f.contentType());
        a.setSizeBytes(f.size());
        a.setSharedWithCustomer(shared);
        a.setUploadedBy(CurrentUser.name());
        Attachment saved = attachments.save(a);
        audits.save(new AuditEntry(defectId, "FILE_ADDED", f.originalName(), CurrentUser.name()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Attachment get(Long defectId, Long id) {
        return attachments.findById(id)
                .filter(a -> a.getDefectId().equals(defectId))
                .orElseThrow(() -> new NotFoundException("File " + id + " not found on defect " + defectId));
    }

    public void delete(Long defectId, Long id) {
        Defect d = requireDefect(defectId);
        if (d.getStatus() == DefectStatus.CLOSED)
            throw new BusinessRuleException("Defect " + defectId + " is closed and cannot be changed");
        Attachment a = get(defectId, id);
        attachments.delete(a);
        storage.delete(a.getStoredName());
        audits.save(new AuditEntry(defectId, "FILE_REMOVED", a.getOriginalName(), CurrentUser.name()));
    }

    // Called when an OPEN defect is deleted
    public void deleteAllForDefect(Long defectId) {
        List<Attachment> all = attachments.findByDefectIdOrderByUploadedAtDesc(defectId);
        for (Attachment a : all) storage.delete(a.getStoredName());
        attachments.deleteAll(all);
    }

    @Transactional(readOnly = true)
    public List<Attachment> sharedFor(Long defectId) {
        return attachments.findByDefectIdAndSharedWithCustomerTrue(defectId);
    }

    // Public access: only with a valid, unexpired report token, and only for files marked "show to customer"
    @Transactional(readOnly = true)
    public Attachment publicGet(String token, Long id) {
        Defect d = defects.findByShareToken(token)
                .filter(x -> x.getShareExpiresAt() != null && x.getShareExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new NotFoundException("This report link is invalid or has expired"));
        return attachments.findById(id)
                .filter(a -> a.getDefectId().equals(d.getId()) && a.isSharedWithCustomer())
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    private Defect requireDefect(Long defectId) {
        return defects.findById(defectId)
                .orElseThrow(() -> new NotFoundException("Defect " + defectId + " not found"));
    }
}
