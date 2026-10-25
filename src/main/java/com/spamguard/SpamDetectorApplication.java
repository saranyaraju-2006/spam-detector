package com.spamguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpamDetectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpamDetectorApplication.class, args);
    }
}
