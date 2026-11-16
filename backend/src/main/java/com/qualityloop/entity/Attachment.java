package com.qualityloop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// A photo or PDF kept as proof for one defect. The file is on disk, this row describes it.
@Entity
@Table(name = "attachment", indexes = @Index(name = "idx_attachment_defect", columnList = "defect_id"))
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "defect_id", nullable = false)
    private Long defectId;

    private String originalName;

    // Random name on disk. Never sent to the browser.
    @JsonIgnore
    private String storedName;

    private String contentType;
    private long sizeBytes;

    // Only files ticked "show to customer" appear on the shared report
    private boolean sharedWithCustomer;

    private String uploadedBy;
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getDefectId() { return defectId; }
    public void setDefectId(Long defectId) { this.defectId = defectId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public boolean isSharedWithCustomer() { return sharedWithCustomer; }
    public void setSharedWithCustomer(boolean sharedWithCustomer) { this.sharedWithCustomer = sharedWithCustomer; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
