package com.qualityloop.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// What a customer is allowed to see. Internal remarks and audit history are left out on purpose.
public record ReportView(Long id, String title, String partNumber, String partName, String source,
                         Integer quantityAffected, LocalDate detectedOn, String detectedBy,
                         String severity, String status, String description, String containmentAction,
                         String rootCause, LocalDateTime createdAt, LocalDateTime closedAt,
                         List<ActionView> actions, List<PhotoView> photos) {

    public record ActionView(String description, String owner, LocalDate dueDate, String status) {
    }

    // Images the team chose to show to the customer
    public record PhotoView(Long id, String name) {
    }
}
