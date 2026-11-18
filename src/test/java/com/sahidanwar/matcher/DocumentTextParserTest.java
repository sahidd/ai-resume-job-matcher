package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.Filenames;
import com.sahidanwar.matcher.infrastructure.parsing.DocumentTextParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextParserTest {

    @Test
    void readsPlainTextAndIgnoresAnEmptyFile() {
        DocumentTextParser parser = new DocumentTextParser();
        byte[] text = "Java and Kafka".getBytes(StandardCharsets.UTF_8);
        assertThat(parser.parse(text, "resume.txt")).contains("Java and Kafka");
        assertThat(parser.parse(new byte[0], "empty.txt")).isEmpty();
    }

    @Test
    void keepsOnlyTheFileName() {
        assertThat(Filenames.sanitize("C:\\incoming\\resume.pdf")).isEqualTo("resume.pdf");
        assertThat(Filenames.sanitize("  ")).isEqualTo("upload.txt");
    }
}
