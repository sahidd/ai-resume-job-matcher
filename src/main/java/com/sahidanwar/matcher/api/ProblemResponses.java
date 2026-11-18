package com.sahidanwar.matcher.api;

import com.sahidanwar.matcher.domain.DomainException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ProblemResponses {

    public ProblemDetail problem(DomainException exception, URI instance) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(exception.status()), exception.getMessage());
        detail.setTitle(title(exception.code()));
        detail.setType(URI.create("https://matcher.sahidanwar.dev/problems/" + exception.code()));
        detail.setInstance(instance);
        return detail;
    }

    private static String title(String code) {
        return switch (code) {
            case "not-found" -> "Not found";
            case "username-taken" -> "Username taken";
            case "invalid-credentials" -> "Invalid credentials";
            case "wrong-document-kind" -> "Wrong document kind";
            case "document-not-ready" -> "Document not ready";
            case "unauthorized" -> "Unauthorized";
            default -> "Request failed";
        };
    }
}
