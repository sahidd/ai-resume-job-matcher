package com.sahidanwar.matcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class StoredDocument {

    @Id
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 80)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentKind kind;

    @Column(nullable = false, length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "text_content")
    private String textContent;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StoredDocument() {
    }

    public static StoredDocument pending(String ownerName, DocumentKind kind, String filename) {
        StoredDocument document = new StoredDocument();
        document.id = UUID.randomUUID();
        document.ownerName = ownerName;
        document.kind = kind;
        document.filename = filename;
        document.status = DocumentStatus.PENDING;
        document.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        return document;
    }

    public void markReady(String text) {
        this.textContent = text;
        this.errorMessage = null;
        this.status = DocumentStatus.READY;
    }

    public void markFailed(String message) {
        this.errorMessage = clip(message);
        this.status = DocumentStatus.FAILED;
    }

    private static String clip(String message) {
        if (message == null || message.isBlank()) {
            return "Document processing failed.";
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 1000 ? oneLine : oneLine.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public DocumentKind getKind() {
        return kind;
    }

    public String getFilename() {
        return filename;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getTextContent() {
        return textContent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
