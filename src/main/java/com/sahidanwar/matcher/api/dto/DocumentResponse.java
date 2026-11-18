package com.sahidanwar.matcher.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sahidanwar.matcher.domain.DocumentKind;
import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.domain.StoredDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String owner,
        DocumentKind kind,
        String filename,
        DocumentStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) String errorMessage,
        OffsetDateTime createdAt
) {
    public static DocumentResponse from(StoredDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getOwnerName(),
                document.getKind(),
                document.getFilename(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getCreatedAt());
    }
}
