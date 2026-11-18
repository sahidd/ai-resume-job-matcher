package com.sahidanwar.matcher.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SkillDictionary {

    private static final List<String> SKILLS = List.of(
            "java", "spring", "spring boot", "kafka", "postgresql", "mysql", "redis", "docker",
            "kubernetes", "aws", "azure", "microservices", "junit", "flyway", "hibernate", "rest",
            "oauth", "jwt", "sql", "git", "maven", "linux", "python", "react", "javascript",
            "typescript", "mongodb", "rabbitmq", "elasticsearch", "terraform", "jenkins",
            "github actions", "opentelemetry", "grafana");

    private static final List<Term> TERMS = SKILLS.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(skill -> new Term(skill, Pattern.compile("(?<![a-z0-9])" + Pattern.quote(skill) + "(?![a-z0-9])")))
            .toList();

    private SkillDictionary() {
    }

    public static List<String> find(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (Term term : TERMS) {
            if (term.pattern.matcher(haystack).find()) {
                found.add(term.skill);
            }
        }
        found.sort(String::compareTo);
        return List.copyOf(found);
    }

    public record Gap(List<String> matched, List<String> missing) {
    }

    public static Gap compare(String resume, String job) {
        List<String> resumeSkills = find(resume);
        List<String> jobSkills = find(job);
        List<String> matched = jobSkills.stream().filter(resumeSkills::contains).toList();
        List<String> missing = jobSkills.stream().filter(skill -> !resumeSkills.contains(skill)).toList();
        return new Gap(matched, missing);
    }

    private record Term(String skill, Pattern pattern) {
    }
}
