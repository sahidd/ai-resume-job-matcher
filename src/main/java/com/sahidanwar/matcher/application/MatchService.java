package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.api.dto.MatchResponse;
import com.sahidanwar.matcher.api.dto.SearchHit;
import com.sahidanwar.matcher.api.dto.SearchResponse;
import com.sahidanwar.matcher.domain.DocumentKind;
import com.sahidanwar.matcher.domain.DocumentStatus;
import com.sahidanwar.matcher.domain.StoredDocument;
import com.sahidanwar.matcher.infrastructure.persistence.StoredDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    private static final String SUGGESTION_SYSTEM = """
            You help a candidate close a skill gap for one job.
            Use only the missing skills provided. Do not invent requirements.
            If nothing is missing, say the resume covers the skills this job asks for.
            Keep the answer under 80 words.
            """;

    private final DocumentLookup lookup;
    private final StoredDocumentRepository documents;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final ObjectProvider<TextGenerator> textGenerator;
    private final AtomicInteger computations = new AtomicInteger();

    public MatchService(DocumentLookup lookup, StoredDocumentRepository documents, EmbeddingModel embeddingModel,
                        VectorStore vectorStore, ObjectProvider<TextGenerator> textGenerator) {
        this.lookup = lookup;
        this.documents = documents;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.textGenerator = textGenerator;
    }

    public int computations() {
        return computations.get();
    }

    @Cacheable(cacheNames = "matches", key = "#owner + ':' + #resumeId + ':' + #jobId")
    public MatchResponse match(String owner, UUID resumeId, UUID jobId) {
        computations.incrementAndGet();
        StoredDocument resume = lookup.requireReady(owner, resumeId, DocumentKind.RESUME);
        StoredDocument job = lookup.requireReady(owner, jobId, DocumentKind.JOB);
        SkillDictionary.Gap gap = SkillDictionary.compare(resume.getTextContent(), job.getTextContent());
        double cosine = Vectors.cosine(
                embeddingModel.embed(resume.getTextContent()),
                embeddingModel.embed(job.getTextContent()));
        int jobSkillCount = gap.matched().size() + gap.missing().size();
        Suggestion suggestion = suggest(gap);
        return new MatchResponse(
                resumeId,
                jobId,
                MatchScorer.score(cosine, gap.matched().size(), jobSkillCount),
                gap.matched(),
                gap.missing(),
                suggestion.text(),
                suggestion.source());
    }

    public SearchResponse similar(String owner, UUID sourceId, DocumentKind sourceKind, DocumentKind targetKind, int limit) {
        StoredDocument source = lookup.requireReady(owner, sourceId, sourceKind);
        int capped = Math.max(1, Math.min(5, limit));
        FilterExpressionBuilder filters = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(source.getTextContent())
                .topK(40)
                .similarityThreshold(0.0)
                .filterExpression(filters.and(filters.eq("owner", owner), filters.eq("kind", targetKind.name())).build())
                .build();
        List<Document> hits = vectorStore.similaritySearch(request);
        Map<UUID, Double> best = new HashMap<>();
        if (hits != null) {
            for (Document hit : hits) {
                Object rawId = hit.getMetadata().get("documentId");
                if (rawId == null) {
                    continue;
                }
                UUID documentId = UUID.fromString(rawId.toString());
                if (documentId.equals(sourceId)) {
                    continue;
                }
                double score = hit.getScore() == null ? 0 : hit.getScore();
                best.merge(documentId, score, Math::max);
            }
        }
        List<SearchHit> results = new ArrayList<>();
        best.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(capped)
                .forEach(entry -> documents.findByIdAndOwnerName(entry.getKey(), owner)
                        .filter(document -> document.getKind() == targetKind && document.getStatus() == DocumentStatus.READY)
                        .ifPresent(document -> results.add(new SearchHit(
                                document.getId(),
                                document.getFilename(),
                                MatchScorer.roundSimilarity(entry.getValue())))));
        return new SearchResponse(sourceId, results);
    }

    private Suggestion suggest(SkillDictionary.Gap gap) {
        String fallback = SuggestionWriter.fallback(gap.missing());
        TextGenerator generator = textGenerator.getIfAvailable();
        if (generator == null) {
            return new Suggestion(fallback, "fallback");
        }
        try {
            String generated = generator.generate(SUGGESTION_SYSTEM, """
                    Matched skills: %s
                    Missing skills: %s
                    """.formatted(gap.matched(), gap.missing()));
            if (generated == null || generated.isBlank()) {
                return new Suggestion(fallback, "fallback");
            }
            return new Suggestion(generated.trim(), "openai");
        } catch (RuntimeException exception) {
            log.warn("Suggestion model failed: {}", exception.toString());
            return new Suggestion(fallback, "fallback");
        }
    }

    private record Suggestion(String text, String source) {
    }
}
