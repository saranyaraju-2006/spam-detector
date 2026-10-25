package com.spamguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One table for both users and admins. The "role" column (USER / ADMIN) is the only difference. */
@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String loginId;

    public String passwordHash;
    public String role = "USER";

    // Mailbox connection (IMAP). Encrypt mailAppPassword before going to production.
    public String mailAddress;
    public String mailAppPassword;
    public String imapHost = "imap.gmail.com";
    public boolean monitoring = false;
    public long lastUid = 0;

    public LocalDateTime createdAt = LocalDateTime.now();
}
