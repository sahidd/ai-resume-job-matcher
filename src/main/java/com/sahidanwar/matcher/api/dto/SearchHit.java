package com.sahidanwar.matcher.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SearchHit(UUID documentId, String filename, BigDecimal score) {
}
