package com.sahidanwar.matcher.infrastructure.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "none", matchIfMissing = true)
public class HashEmbeddingModel implements EmbeddingModel {

    public static final int DIMENSIONS = 128;

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+");

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> results = new ArrayList<>();
        List<String> inputs = request.getInstructions();
        for (int i = 0; i < inputs.size(); i++) {
            results.add(new Embedding(vector(inputs.get(i)), i));
        }
        return new EmbeddingResponse(results);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText() == null ? "" : document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    static float[] vector(String text) {
        float[] values = new float[DIMENSIONS];
        Matcher matcher = TOKEN.matcher(text == null ? "" : text.toLowerCase(Locale.ROOT));
        boolean any = false;
        while (matcher.find()) {
            int bucket = Math.floorMod(matcher.group().hashCode(), DIMENSIONS);
            values[bucket] += 1.0f;
            any = true;
        }
        if (!any) {
            values[0] = 1.0f;
        }
        double norm = 0;
        for (float value : values) {
            norm += value * (double) value;
        }
        float scale = (float) Math.sqrt(norm);
        if (scale == 0) {
            return values;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] /= scale;
        }
        return values;
    }
}
