package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void collapsesWhitespaceAndKeepsAShortResumeWhole() {
        assertThat(chunker.chunk("Java \n\n  Kafka")).containsExactly("Java Kafka");
        assertThat(chunker.chunk("   ")).isEmpty();
    }

    @Test
    void overlapsWindowsOfLongText() {
        String text = "a".repeat(900);
        List<String> chunks = chunker.chunk(text);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(800);
        assertThat(chunks.get(1)).isEqualTo(text.substring(700));
    }
}
