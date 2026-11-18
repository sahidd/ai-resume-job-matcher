package com.sahidanwar.matcher.api;

import com.sahidanwar.matcher.api.dto.RegisterRequest;
import com.sahidanwar.matcher.api.dto.TokenRequest;
import com.sahidanwar.matcher.api.dto.TokenResponse;
import com.sahidanwar.matcher.api.dto.UserResponse;
import com.sahidanwar.matcher.application.AuthService;
import com.sahidanwar.matcher.application.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return new UserResponse(authService.register(request.username(), request.password()));
    }

    @PostMapping("/token")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        authService.checkCredentials(request.username(), request.password());
        return tokenService.issue(request.username());
    }
}
