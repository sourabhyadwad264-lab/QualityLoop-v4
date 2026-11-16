package com.qualityloop.repository;

import com.qualityloop.entity.Drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {
    List<Drawing> findAllByOrderByPartNumberAsc();
    Optional<Drawing> findByPartNumberIgnoreCase(String partNumber);
    boolean existsByPartNumberIgnoreCase(String partNumber);
}
