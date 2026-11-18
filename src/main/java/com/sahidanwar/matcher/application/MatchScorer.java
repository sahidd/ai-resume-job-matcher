package com.sahidanwar.matcher.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MatchScorer {

    private MatchScorer() {
    }

    public static BigDecimal score(double cosine, int matched, int jobSkills) {
        double similarity = clamp(cosine);
        double raw = jobSkills == 0 ? similarity : (0.6 * similarity) + (0.4 * ((double) matched / jobSkills));
        long hundredths = Math.round(clamp(raw) * 100.0);
        return BigDecimal.valueOf(hundredths, 2);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }

    public static BigDecimal roundSimilarity(double value) {
        long hundredths = Math.round(clamp(value) * 100.0);
        return BigDecimal.valueOf(hundredths, 2);
    }
}
