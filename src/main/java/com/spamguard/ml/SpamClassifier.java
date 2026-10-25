package com.spamguard.ml;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Multinomial Naive Bayes spam classifier (pure Java machine learning, no extra libraries).
 * Trains at startup from a CSV file with lines:  label,text   (label = spam or ham).
 */
@Component
public class SpamClassifier {
    private static final Logger log = LoggerFactory.getLogger(SpamClassifier.class);

    public record Result(boolean spam, double score) {}

    private final Map<String, Integer> spamWords = new HashMap<>();
    private final Map<String, Integer> hamWords = new HashMap<>();
    private final Set<String> vocab = new HashSet<>();
    private int spamTotal, hamTotal, spamDocs, hamDocs;

    @Value("${spam.dataset.path:}")
    private String externalPath;

    @Value("${spam.threshold:0.5}")
    private double threshold;

    @PostConstruct
    public void init() throws IOException {
        InputStream in;
        if (externalPath != null && !externalPath.isBlank() && Files.exists(Path.of(externalPath))) {
            in = Files.newInputStream(Path.of(externalPath));
            log.info("Training from external dataset {}", externalPath);
        } else {
            in = new ClassPathResource("spam_dataset.csv").getInputStream();
            log.info("Training from built-in sample dataset");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf(',');
                if (i < 0) continue;
                String label = line.substring(0, i).trim().replace("\"", "").toLowerCase();
                if (!label.equals("spam") && !label.equals("ham")) continue; // skips header rows
                train(label.equals("spam"), line.substring(i + 1));
            }
        }
        log.info("Classifier ready: {} spam docs, {} ham docs, {} words", spamDocs, hamDocs, vocab.size());
    }

    public synchronized void train(boolean spam, String text) {
        for (String t : tokenize(text)) {
            vocab.add(t);
            if (spam) { spamWords.merge(t, 1, Integer::sum); spamTotal++; }
            else { hamWords.merge(t, 1, Integer::sum); hamTotal++; }
        }
        if (spam) spamDocs++; else hamDocs++;
    }

    public synchronized Result classify(String text) {
        double docs = spamDocs + hamDocs;
        double logSpam = Math.log((spamDocs + 1.0) / (docs + 2.0));
        double logHam = Math.log((hamDocs + 1.0) / (docs + 2.0));
        int v = vocab.size();
        for (String t : tokenize(text)) {
            logSpam += Math.log((spamWords.getOrDefault(t, 0) + 1.0) / (spamTotal + v + 1.0));
            logHam += Math.log((hamWords.getOrDefault(t, 0) + 1.0) / (hamTotal + v + 1.0));
        }
        double max = Math.max(logSpam, logHam);
        double ps = Math.exp(logSpam - max);
        double ph = Math.exp(logHam - max);
        double p = ps / (ps + ph);
        return new Result(p >= threshold, p);
    }

    private List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String w : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (w.length() >= 2) out.add(w);
        }
        return out;
    }
}
