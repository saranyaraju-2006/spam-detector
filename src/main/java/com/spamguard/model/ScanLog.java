package com.spamguard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** History entry. The message BODY is never stored. Subject is only shown to the owner, never to admin. */
@Entity
@Table(name = "scan_log")
public class ScanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long userId;
    public String loginId;
    public String sender;
    @Column(length = 500)
    public String subject;
    public String label;      // SPAM or SAFE
    public double score;      // spam probability 0..1
    public String source;     // EMAIL or MANUAL
    public LocalDateTime scannedAt = LocalDateTime.now();
}
