package com.sahidanwar.matcher.config;

import com.sahidanwar.matcher.domain.AppUser;
import com.sahidanwar.matcher.infrastructure.persistence.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoUserSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsByUsername("demo")) {
            users.save(AppUser.create("demo", passwordEncoder.encode("password")));
        }
    }
}
