package com.sahidanwar.matcher.api.dto;

import java.util.List;

public record AskResponse(String answer, List<Citation> citations, String source) {
}
