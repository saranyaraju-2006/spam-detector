package com.spamguard.web;

import com.spamguard.model.AppUser;
import com.spamguard.repo.UserRepository;
import com.spamguard.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(String loginId, String password) {}

    private final UserRepository users;
    private final AuthService auth;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    /** Public sign-up always creates a normal USER. Admin accounts are created only by the server. */
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Credentials c) {
        if (c.loginId() == null || !c.loginId().matches("^[A-Za-z0-9_.-]{3,30}$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID must be 3-30 letters, numbers, . _ -");
        if (c.password() == null || c.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        if (users.existsByLoginId(c.loginId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This ID is already taken");
        AppUser u = new AppUser();
        u.loginId = c.loginId();
        u.passwordHash = encoder.encode(c.password());
        u.role = "USER";
        users.save(u);
        return Map.of("message", "Account created. You can log in now.");
    }

    /** One login for everybody. The role stored on the account decides where the user goes. */
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Credentials c, HttpServletRequest req) {
        AppUser u = users.findByLoginId(c.loginId() == null ? "" : c.loginId()).orElse(null);
        if (u == null || c.password() == null || !encoder.matches(c.password(), u.passwordHash))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID or password");
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute("uid", u.id);
        return Map.of("loginId", u.loginId, "role", u.role);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession s) {
        s.invalidate();
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public Map<String, String> me(HttpSession s) {
        AppUser u = auth.current(s);
        return Map.of("loginId", u.loginId, "role", u.role);
    }
}
