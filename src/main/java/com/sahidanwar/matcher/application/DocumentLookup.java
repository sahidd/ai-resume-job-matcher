package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.domain.DocumentKind;
import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.domain.DomainException;
import com.sahidanwar.matcher.domain.StoredDocument;
import com.sahidanwar.matcher.infrastructure.persistence.StoredDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentLookup {

    private final StoredDocumentRepository documents;

    public DocumentLookup(StoredDocumentRepository documents) {
        this.documents = documents;
    }

    public StoredDocument requireOwned(String owner, UUID id) {
        return documents.findByIdAndOwnerName(id, owner)
                .orElseThrow(() -> new DomainException("not-found", "Document was not found.", 404));
    }

    public StoredDocument requireReady(String owner, UUID id, DocumentKind kind) {
        StoredDocument document = requireOwned(owner, id);
        if (document.getKind() != kind) {
            throw new DomainException("wrong-document-kind",
                    "Expected a " + kind + " but found a " + document.getKind() + ".", 422);
        }
        if (document.getStatus() != DocumentStatus.READY || document.getTextContent() == null
                || document.getTextContent().isBlank()) {
            throw new DomainException("document-not-ready",
                    "Document is " + document.getStatus() + " and cannot be queried yet.", 409);
        }
        return document;
    }
}
