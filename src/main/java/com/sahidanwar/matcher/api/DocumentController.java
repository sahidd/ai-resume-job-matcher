package com.sahidanwar.matcher.api;

import com.sahidanwar.matcher.api.dto.DocumentResponse;
import com.sahidanwar.matcher.application.DocumentProcessor;
import com.sahidanwar.matcher.application.DocumentService;
import com.sahidanwar.matcher.application.Filenames;
import com.sahidanwar.matcher.domain.DocumentKind;
import com.sahidanwar.matcher.domain.DomainException;
import com.sahidanwar.matcher.domain.StoredDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessor processor;

    public DocumentController(DocumentService documentService, DocumentProcessor processor) {
        this.documentService = documentService;
        this.processor = processor;
    }

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentResponse> uploadResume(@RequestPart("file") MultipartFile file) {
        return upload(DocumentKind.RESUME, file);
    }

    @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentResponse> uploadJob(@RequestPart("file") MultipartFile file) {
        return upload(DocumentKind.JOB, file);
    }

    @GetMapping("/documents/{id}")
    DocumentResponse get(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.get(CurrentUser.name(), id));
    }

    private ResponseEntity<DocumentResponse> upload(DocumentKind kind, MultipartFile file) {
        byte[] bytes = read(file);
        // Persist the pending row before the async worker starts, so the worker can see it.
        StoredDocument saved = documentService.createPending(
                CurrentUser.name(), kind, Filenames.sanitize(file.getOriginalFilename()));
        processor.process(saved.getId(), bytes);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(saved));
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new DomainException("unreadable-upload", "The upload could not be read.", 400);
        }
    }
}
