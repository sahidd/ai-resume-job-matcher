package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.MatchScorer;
import com.sahidanwar.matcher.application.SuggestionWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchScorerTest {

    @Test
    void blendsCosineWithTheShareOfJobSkillsCovered() {
        assertThat(MatchScorer.score(1, 1, 2)).isEqualByComparingTo("0.80");
        assertThat(MatchScorer.score(0.333, 0, 0)).isEqualByComparingTo("0.33");
        assertThat(MatchScorer.score(2, 0, 0)).isEqualByComparingTo("1.00");
    }

    @Test
    void fallbackSuggestionsNameTheMissingSkills() {
        assertThat(SuggestionWriter.fallback(List.of()))
                .isEqualTo("The resume covers the skills this job asks for.");
        assertThat(SuggestionWriter.fallback(List.of("kafka", "kubernetes"))).contains("kafka, kubernetes");
    }
}
