package com.sahidanwar.matcher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AskRequest(@NotNull UUID documentId, @NotBlank @Size(max = 1000) String question) {
}
