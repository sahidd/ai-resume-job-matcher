package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.domain.DocumentKind;
import com.sahidanwar.matcher.domain.DomainException;
import com.sahidanwar.matcher.domain.StoredDocument;
import com.sahidanwar.matcher.infrastructure.persistence.StoredDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentService {

    private final StoredDocumentRepository documents;

    public DocumentService(StoredDocumentRepository documents) {
        this.documents = documents;
    }

    @Transactional
    public StoredDocument createPending(String owner, DocumentKind kind, String filename) {
        return documents.saveAndFlush(StoredDocument.pending(owner, kind, filename));
    }

    @Transactional(readOnly = true)
    public StoredDocument get(String owner, UUID id) {
        return documents.findByIdAndOwnerName(id, owner)
                .orElseThrow(() -> new DomainException("not-found", "Document was not found.", 404));
    }
}
