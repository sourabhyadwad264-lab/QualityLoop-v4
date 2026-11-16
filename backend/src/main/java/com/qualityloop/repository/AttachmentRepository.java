package com.qualityloop.repository;

import com.qualityloop.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByDefectIdOrderByUploadedAtDesc(Long defectId);
    List<Attachment> findByDefectIdAndSharedWithCustomerTrue(Long defectId);
    long countByDefectId(Long defectId);
}
