package com.spamguard.service;

import com.spamguard.model.AppUser;
import com.spamguard.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public AppUser current(HttpSession s) {
        Object id = s.getAttribute("uid");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        return users.findById((Long) id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in"));
    }

    public AppUser admin(HttpSession s) {
        AppUser u = current(s);
        if (!"ADMIN".equals(u.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        return u;
    }
}
