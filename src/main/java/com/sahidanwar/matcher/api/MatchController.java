package com.sahidanwar.matcher.api;

import com.sahidanwar.matcher.api.dto.MatchRequest;
import com.sahidanwar.matcher.api.dto.MatchResponse;
import com.sahidanwar.matcher.api.dto.SearchResponse;
import com.sahidanwar.matcher.application.MatchService;
import com.sahidanwar.matcher.domain.DocumentKind;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/matches")
    MatchResponse match(@Valid @RequestBody MatchRequest request) {
        return matchService.match(CurrentUser.name(), request.resumeId(), request.jobId());
    }

    @GetMapping("/resumes/{id}/jobs")
    SearchResponse jobsForResume(@PathVariable UUID id, @RequestParam(defaultValue = "5") int limit) {
        return matchService.similar(CurrentUser.name(), id, DocumentKind.RESUME, DocumentKind.JOB, limit);
    }

    @GetMapping("/jobs/{id}/resumes")
    SearchResponse resumesForJob(@PathVariable UUID id, @RequestParam(defaultValue = "5") int limit) {
        return matchService.similar(CurrentUser.name(), id, DocumentKind.JOB, DocumentKind.RESUME, limit);
    }
}
