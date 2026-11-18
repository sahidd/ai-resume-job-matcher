package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.domain.StoredDocument;
import com.sahidanwar.matcher.infrastructure.parsing.DocumentTextParser;
import com.sahidanwar.matcher.infrastructure.persistence.StoredDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private final StoredDocumentRepository documents;
    private final DocumentTextParser parser;
    private final TextChunker chunker;
    private final VectorStore vectorStore;
    private final TransactionTemplate transactionTemplate;

    public DocumentProcessor(StoredDocumentRepository documents, DocumentTextParser parser, TextChunker chunker,
                              VectorStore vectorStore, TransactionTemplate transactionTemplate) {
        this.documents = documents;
        this.parser = parser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.transactionTemplate = transactionTemplate;
    }

    @Async
    public void process(UUID id, byte[] content) {
        try {
            StoredDocument pending = awaitPending(id);
            if (pending == null) {
                log.error("Document {} was not pending when indexing started", id);
                return;
            }
            String text = parser.parse(content, pending.getFilename());
            if (text.isBlank()) {
                markFailed(id, "No text could be extracted.");
                return;
            }
            List<String> chunks = chunker.chunk(text);
            List<Document> indexed = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                // PgVectorStore's default id column is uuid. A composite id such as
                // "{documentId}:{chunk}" fails UUID parsing and the document never becomes READY.
                indexed.add(new Document(UUID.randomUUID().toString(), chunks.get(i), Map.of(
                        "documentId", id.toString(),
                        "kind", pending.getKind().name(),
                        "owner", pending.getOwnerName(),
                        "chunk", Integer.toString(i))));
            }
            vectorStore.add(indexed);
            markReady(id, text);
            log.info("Indexed document {} into {} chunks", id, indexed.size());
        } catch (RuntimeException exception) {
            log.warn("Document {} failed processing: {}", id, exception.toString());
            markFailed(id, exception.getMessage());
        }
    }

    private StoredDocument awaitPending(UUID id) {
        for (int attempt = 0; attempt < 20; attempt++) {
            StoredDocument document = documents.findById(id).orElse(null);
            if (document != null && document.getStatus() == DocumentStatus.PENDING) {
                return document;
            }
            if (document != null) {
                return null;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private void markReady(UUID id, String text) {
        transactionTemplate.executeWithoutResult(status -> {
            StoredDocument document = documents.findById(id).orElse(null);
            if (document == null || document.getStatus() != DocumentStatus.PENDING) {
                return;
            }
            document.markReady(text);
        });
    }

    private void markFailed(UUID id, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            StoredDocument document = documents.findById(id).orElse(null);
            if (document == null || document.getStatus() != DocumentStatus.PENDING) {
                return;
            }
            document.markFailed(message);
        });
    }
}
