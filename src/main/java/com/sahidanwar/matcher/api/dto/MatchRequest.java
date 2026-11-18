package com.sahidanwar.matcher.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MatchRequest(@NotNull UUID resumeId, @NotNull UUID jobId) {
}
