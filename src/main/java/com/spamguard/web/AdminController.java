package com.spamguard.web;

import com.spamguard.model.AppUser;
import com.spamguard.model.ScanLog;
import com.spamguard.repo.ScanLogRepository;
import com.spamguard.repo.UserRepository;
import com.spamguard.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin can see accounts and scan history (who, when, sender, SPAM/SAFE, score).
 * Admin can NOT see subjects or message bodies - those fields are never returned here.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService auth;
    private final UserRepository users;
    private final ScanLogRepository logs;

    public AdminController(AuthService auth, UserRepository users, ScanLogRepository logs) {
        this.auth = auth;
        this.users = users;
        this.logs = logs;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(HttpSession s) {
        auth.admin(s);
        return users.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("loginId", u.loginId);
            m.put("role", u.role);
            m.put("mailAddress", u.mailAddress);
            m.put("monitoring", u.monitoring);
            m.put("createdAt", u.createdAt);
            m.put("total", logs.countByUserId(u.id));
            m.put("spam", logs.countByUserIdAndLabel(u.id, "SPAM"));
            return m;
        }).toList();
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(@RequestParam(required = false) String loginId, HttpSession s) {
        auth.admin(s);
        List<ScanLog> list = (loginId == null || loginId.isBlank())
                ? logs.findTop200ByOrderByScannedAtDesc()
                : logs.findTop200ByLoginIdOrderByScannedAtDesc(loginId.trim());
        return list.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("loginId", l.loginId);
            m.put("sender", l.sender);
            m.put("label", l.label);
            m.put("score", l.score);
            m.put("source", l.source);
            m.put("scannedAt", l.scannedAt);
            return m;     // no subject, no body
        }).toList();
    }
}
