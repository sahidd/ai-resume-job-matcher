package com.sahidanwar.matcher.infrastructure.parsing;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentTextParser {

    private static final int MAX_CHARS = 200_000;

    public String parse(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            return "";
        }
        Tika tika = new Tika();
        tika.setMaxStringLength(MAX_CHARS);
        Metadata metadata = new Metadata();
        if (filename != null && !filename.isBlank()) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        }
        try (InputStream input = new ByteArrayInputStream(content)) {
            String text = tika.parseToString(input, metadata);
            if (text == null) {
                return "";
            }
            return text.replace("\u0000", "").trim();
        } catch (IOException | TikaException exception) {
            throw new IllegalArgumentException("The file could not be parsed.", exception);
        }
    }
}
