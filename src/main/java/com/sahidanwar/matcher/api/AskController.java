package com.sahidanwar.matcher.api;

import com.sahidanwar.matcher.api.dto.AskRequest;
import com.sahidanwar.matcher.api.dto.AskResponse;
import com.sahidanwar.matcher.application.AskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping("/ask")
    AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(CurrentUser.name(), request.documentId(), request.question());
    }
}
