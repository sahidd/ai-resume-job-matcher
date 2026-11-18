package com.sahidanwar.matcher.api.dto;

import java.util.List;
import java.util.UUID;

public record SearchResponse(UUID documentId, List<SearchHit> results) {
}
