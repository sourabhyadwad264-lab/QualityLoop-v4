package com.qualityloop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One drawing per part number. Updates never overwrite: each update is a new revision.
@Entity
@Table(name = "drawing")
public class Drawing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_number", nullable = false, unique = true)
    private String partNumber;

    @Column(nullable = false)
    private String title;

    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Newest revision first
    @OneToMany(mappedBy = "drawing", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("revisionNumber DESC")
    private List<DrawingRevision> revisions = new ArrayList<>();

    public Long getId() { return id; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<DrawingRevision> getRevisions() { return revisions; }
}
