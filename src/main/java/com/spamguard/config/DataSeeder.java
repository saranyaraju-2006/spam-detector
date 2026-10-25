package com.spamguard.config;

import com.spamguard.model.AppUser;
import com.spamguard.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup if it does not exist. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;

    @Value("${admin.id}")
    private String adminId;
    @Value("${admin.password}")
    private String adminPassword;

    public DataSeeder(UserRepository users) {
        this.users = users;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByLoginId(adminId)) {
            AppUser a = new AppUser();
            a.loginId = adminId;
            a.passwordHash = new BCryptPasswordEncoder().encode(adminPassword);
            a.role = "ADMIN";
            users.save(a);
            System.out.println(">>> Admin account created: " + adminId);
        }
    }
}
