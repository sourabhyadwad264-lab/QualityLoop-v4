package com.qualityloop.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ActionRequest(
        @NotBlank(message = "Action description is required") String description,
        String owner,
        @FutureOrPresent(message = "Due date cannot be in the past") LocalDate dueDate) {
}
