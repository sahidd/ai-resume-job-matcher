package com.sahidanwar.matcher.application;

import com.sahidanwar.matcher.domain.AppUser;
import com.sahidanwar.matcher.domain.DomainException;
import com.sahidanwar.matcher.infrastructure.persistence.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String register(String username, String password) {
        if (users.existsByUsername(username)) {
            throw new DomainException("username-taken", "That username is already registered.", 409);
        }
        users.save(AppUser.create(username, passwordEncoder.encode(password)));
        return username;
    }

    @Transactional(readOnly = true)
    public void checkCredentials(String username, String password) {
        boolean matches = users.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .orElse(false);
        if (!matches) {
            throw new DomainException("invalid-credentials", "Invalid username or password.", 401);
        }
    }
}
