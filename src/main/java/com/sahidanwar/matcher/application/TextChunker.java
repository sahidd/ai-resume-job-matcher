package com.sahidanwar.matcher.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    static final int SIZE = 800;
    static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        if (text == null) {
            return List.of();
        }
        String normalized = text.replace('\u0000', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.length() <= SIZE) {
            return List.of(normalized);
        }
        int step = SIZE - OVERLAP;
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + SIZE, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
        }
        return chunks;
    }
}
