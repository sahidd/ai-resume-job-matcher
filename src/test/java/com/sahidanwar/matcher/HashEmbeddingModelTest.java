package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.Vectors;
import com.sahidanwar.matcher.infrastructure.ai.HashEmbeddingModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HashEmbeddingModelTest {

    @Test
    void relatedResumesAreCloserThanUnrelatedText() {
        HashEmbeddingModel model = new HashEmbeddingModel();
        float[] java = model.embed("Java Spring Kafka PostgreSQL microservices");
        float[] related = model.embed("Spring Boot Kafka and Java services");
        float[] baking = model.embed("sourdough baking bread oven starter");

        assertThat(java).hasSize(HashEmbeddingModel.DIMENSIONS);
        assertThat(model.dimensions()).isEqualTo(128);
        assertThat(Vectors.cosine(java, related)).isGreaterThan(Vectors.cosine(java, baking));
        double norm = 0;
        for (float value : java) {
            norm += value * (double) value;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1, within(0.001));
    }
}
