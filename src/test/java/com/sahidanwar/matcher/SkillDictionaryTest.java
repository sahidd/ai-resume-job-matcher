package com.sahidanwar.matcher;

import com.sahidanwar.matcher.application.SkillDictionary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillDictionaryTest {

    @Test
    void reportsMatchedAndMissingSkills() {
        SkillDictionary.Gap gap = SkillDictionary.compare(
                "Built Spring Boot services and Java APIs",
                "Need Java, Spring Boot, and Kafka");
        assertThat(gap.matched()).contains("java", "spring", "spring boot");
        assertThat(gap.missing()).containsExactly("kafka");
    }

    @Test
    void doesNotTreatJavascriptAsJava() {
        assertThat(SkillDictionary.find("javascript engineer")).contains("javascript").doesNotContain("java");
    }
}
