package com.sahidanwar.matcher.application;

import java.util.List;

public final class SuggestionWriter {

    private SuggestionWriter() {
    }

    public static String fallback(List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return "The resume covers the skills this job asks for.";
        }
        return "Add a short example for each missing skill: " + String.join(", ", missing) + ".";
    }
}
