package com.sahidanwar.matcher.infrastructure.persistence;

import com.sahidanwar.matcher.domain.StoredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoredDocumentRepository extends JpaRepository<StoredDocument, UUID> {

    Optional<StoredDocument> findByIdAndOwnerName(UUID id, String ownerName);
}
