package com.sahidanwar.matcher.api.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
