package com.spamguard.service;

import com.spamguard.ml.SpamClassifier;
import com.spamguard.model.AppUser;
import com.spamguard.model.ScanLog;
import com.spamguard.repo.ScanLogRepository;
import com.spamguard.repo.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Connects to each user's mailbox over IMAP, reads NEW emails, classifies them with the ML model
 * and stores only metadata (sender, subject, label, score). The body is analysed in memory and discarded.
 */
@Service
public class EmailPollingService {
    private static final Logger log = LoggerFactory.getLogger(EmailPollingService.class);

    private final UserRepository users;
    private final ScanLogRepository logs;
    private final SpamClassifier classifier;

    public EmailPollingService(UserRepository users, ScanLogRepository logs, SpamClassifier classifier) {
        this.users = users;
        this.logs = logs;
        this.classifier = classifier;
    }

    @Scheduled(fixedDelayString = "${spam.poll.interval:30000}", initialDelay = 10000)
    public void pollAll() {
        for (AppUser u : users.findByMonitoringTrue()) {
            try {
                poll(u);
            } catch (Exception e) {
                log.warn("Polling failed for {}: {}", u.loginId, e.getMessage());
            }
        }
    }

    public void testConnection(AppUser u) throws MessagingException {
        try (Store store = connect(u)) {
            store.getFolder("INBOX");
        }
    }

    public synchronized void poll(AppUser u) throws Exception {
        try (Store store = connect(u)) {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            UIDFolder uf = (UIDFolder) inbox;

            Message[] msgs;
            if (u.lastUid == 0) {                         // first run: scan the latest 20 emails only
                int n = inbox.getMessageCount();
                if (n == 0) { inbox.close(false); return; }
                msgs = inbox.getMessages(Math.max(1, n - 19), n);
            } else {
                msgs = uf.getMessagesByUID(u.lastUid + 1, UIDFolder.LASTUID);
            }

            long maxUid = u.lastUid;
            for (Message m : msgs) {
                long uid = uf.getUID(m);
                if (uid <= u.lastUid) continue;
                maxUid = Math.max(maxUid, uid);

                String subject = m.getSubject() == null ? "(no subject)" : m.getSubject();
                String sender = "unknown";
                Address[] from = m.getFrom();
                if (from != null && from.length > 0) {
                    sender = from[0] instanceof InternetAddress ia ? ia.getAddress() : from[0].toString();
                }
                String body = bodyText(m);
                if (body.length() > 5000) body = body.substring(0, 5000);

                SpamClassifier.Result r = classifier.classify(subject + " " + body);

                ScanLog entry = new ScanLog();
                entry.userId = u.id;
                entry.loginId = u.loginId;
                entry.sender = sender;
                entry.subject = subject.length() > 490 ? subject.substring(0, 490) : subject;
                entry.label = r.spam() ? "SPAM" : "SAFE";
                entry.score = r.score();
                entry.source = "EMAIL";
                logs.save(entry);
            }
            inbox.close(false);

            if (maxUid != u.lastUid) {
                u.lastUid = maxUid;
                users.save(u);
            }
        }
    }

    private Store connect(AppUser u) throws MessagingException {
        Properties p = new Properties();
        p.put("mail.store.protocol", "imaps");
        p.put("mail.imaps.host", u.imapHost);
        p.put("mail.imaps.port", "993");
        p.put("mail.imaps.ssl.enable", "true");
        p.put("mail.imaps.connectiontimeout", "10000");
        p.put("mail.imaps.timeout", "15000");
        Session session = Session.getInstance(p);
        Store store = session.getStore("imaps");
        store.connect(u.imapHost, u.mailAddress, u.mailAppPassword);
        return store;
    }

    private String bodyText(Part p) throws Exception {
        if (p.isMimeType("text/plain")) return String.valueOf(p.getContent());
        if (p.isMimeType("text/html")) return String.valueOf(p.getContent()).replaceAll("<[^>]+>", " ");
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) sb.append(bodyText(mp.getBodyPart(i))).append(' ');
            return sb.toString();
        }
        return "";
    }
}
