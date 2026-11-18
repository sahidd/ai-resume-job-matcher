package com.sahidanwar.matcher.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MatchResponse(
        UUID resumeId,
        UUID jobId,
        BigDecimal score,
        List<String> matchedSkills,
        List<String> missingSkills,
        String suggestions,
        String suggestionSource
) {
}
