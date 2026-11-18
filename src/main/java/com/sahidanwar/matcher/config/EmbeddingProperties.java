package com.sahidanwar.matcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.embedding")
public class EmbeddingProperties {

    private int dimensions = 128;

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }
}
