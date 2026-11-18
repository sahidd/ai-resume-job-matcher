package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.api.dto.AskResponse;
import com.sahidanwar.matcher.api.dto.Citation;
import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.domain.DomainException;
import com.sahidanwar.matcher.domain.StoredDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);
    private static final String SYSTEM = """
            Answer the question using only the excerpts from this candidate document.
            Cite chunk numbers in square brackets. If the excerpts do not contain the answer, say so.
            Do not invent experience.
            """;

    private final DocumentLookup lookup;
    private final VectorStore vectorStore;
    private final ObjectProvider<TextGenerator> textGenerator;

    public AskService(DocumentLookup lookup, VectorStore vectorStore, ObjectProvider<TextGenerator> textGenerator) {
        this.lookup = lookup;
        this.vectorStore = vectorStore;
        this.textGenerator = textGenerator;
    }

    public AskResponse ask(String owner, UUID documentId, String question) {
        StoredDocument document = lookup.requireOwned(owner, documentId);
        if (document.getStatus() != DocumentStatus.READY) {
            throw new DomainException("document-not-ready",
                    "Document is " + document.getStatus() + " and cannot be queried yet.", 409);
        }
        List<Citation> citations = citations(owner, documentId, question);
        if (citations.isEmpty()) {
            return new AskResponse("This document has no indexed text that matches the question.", List.of(), "rules");
        }
        TextGenerator generator = textGenerator.getIfAvailable();
        if (generator != null) {
            try {
                String generated = generator.generate(SYSTEM, prompt(question, citations));
                if (generated != null && !generated.isBlank()) {
                    return new AskResponse(generated.trim(), citations, "openai");
                }
            } catch (RuntimeException exception) {
                log.warn("Answer model failed: {}", exception.toString());
            }
        }
        return new AskResponse(fallback(citations), citations, "fallback");
    }

    private List<Citation> citations(String owner, UUID documentId, String question) {
        FilterExpressionBuilder filters = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(4)
                .similarityThreshold(0.0)
                .filterExpression(filters.and(
                        filters.eq("owner", owner),
                        filters.eq("documentId", documentId.toString())).build())
                .build();
        List<Document> hits = vectorStore.similaritySearch(request);
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        return hits.stream()
                .sorted(Comparator.comparing(hit -> chunkIndex(hit.getMetadata().get("chunk"))))
                .map(hit -> new Citation(
                        String.valueOf(hit.getMetadata().getOrDefault("chunk", "0")),
                        excerpt(hit.getText())))
                .toList();
    }

    private static int chunkIndex(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String excerpt(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }

    private static String prompt(String question, List<Citation> citations) {
        StringBuilder builder = new StringBuilder();
        builder.append("Question: ").append(question).append("\n");
        for (Citation citation : citations) {
            builder.append("Chunk ").append(citation.chunk()).append(": ").append(citation.excerpt()).append("\n");
        }
        return builder.toString();
    }

    private static String fallback(List<Citation> citations) {
        StringBuilder answer = new StringBuilder("A language model is not configured. Closest indexed excerpts:");
        for (Citation citation : citations) {
            answer.append(" [chunk ").append(citation.chunk()).append("] ").append(citation.excerpt());
        }
        return answer.toString();
    }
}
