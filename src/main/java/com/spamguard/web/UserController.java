package com.spamguard.web;

import com.spamguard.ml.SpamClassifier;
import com.spamguard.model.AppUser;
import com.spamguard.model.ScanLog;
import com.spamguard.repo.ScanLogRepository;
import com.spamguard.repo.UserRepository;
import com.spamguard.service.AuthService;
import com.spamguard.service.EmailPollingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    public record MailReq(String mailAddress, String appPassword, String imapHost) {}
    public record CheckReq(String text) {}

    private final AuthService auth;
    private final UserRepository users;
    private final ScanLogRepository logs;
    private final SpamClassifier classifier;
    private final EmailPollingService poller;

    public UserController(AuthService auth, UserRepository users, ScanLogRepository logs,
                          SpamClassifier classifier, EmailPollingService poller) {
        this.auth = auth;
        this.users = users;
        this.logs = logs;
        this.classifier = classifier;
        this.poller = poller;
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpSession s) {
        AppUser u = auth.current(s);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loginId", u.loginId);
        m.put("mailAddress", u.mailAddress);
        m.put("monitoring", u.monitoring);
        return m;   // the mail password is never sent back to the browser
    }

    @PostMapping("/mail")
    public Map<String, String> connect(@RequestBody MailReq r, HttpSession s) {
        AppUser u = auth.current(s);
        if (r.mailAddress() == null || r.appPassword() == null || r.imapHost() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fill in all fields");
        u.mailAddress = r.mailAddress().trim();
        u.mailAppPassword = r.appPassword().trim();
        u.imapHost = r.imapHost().trim();
        try {
            poller.testConnection(u);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not connect to the mailbox. Check address, app password and IMAP setting. (" + e.getMessage() + ")");
        }
        u.monitoring = true;
        u.lastUid = 0;
        users.save(u);
        try {
            poller.poll(u);     // scan the latest emails immediately
        } catch (Exception ignored) {
        }
        return Map.of("message", "Mailbox connected. Real-time monitoring started.");
    }

    @PostMapping("/mail/stop")
    public Map<String, String> stop(HttpSession s) {
        AppUser u = auth.current(s);
        u.monitoring = false;
        users.save(u);
        return Map.of("message", "Monitoring stopped.");
    }

    @GetMapping("/history")
    public List<ScanLog> history(HttpSession s) {
        AppUser u = auth.current(s);
        return logs.findTop100ByUserIdOrderByScannedAtDesc(u.id);
    }

    /** Paste any email text and check it instantly. */
    @PostMapping("/check")
    public Map<String, Object> check(@RequestBody CheckReq r, HttpSession s) {
        AppUser u = auth.current(s);
        if (r.text() == null || r.text().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paste some email text first");
        SpamClassifier.Result res = classifier.classify(r.text());
        ScanLog e = new ScanLog();
        e.userId = u.id;
        e.loginId = u.loginId;
        e.sender = "-";
        e.subject = "(manual check)";
        e.label = res.spam() ? "SPAM" : "SAFE";
        e.score = res.score();
        e.source = "MANUAL";
        logs.save(e);
        return Map.of("label", e.label, "score", e.score);
    }
}
