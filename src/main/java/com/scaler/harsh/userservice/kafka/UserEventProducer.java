package com.scaler.harsh.userservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(Integer userId, String username, String email) {
        // Formatted as a real welcome email would be — Notification Service renders this as-is.
        // Still a mock: nothing here actually sends mail.
        String message = "To: " + email + "\n"
                + "Subject: Welcome to Scaler Capstone Shop!\n\n"
                + "Hi " + username + ",\n\n"
                + "Thanks for creating an account with us (userId: " + userId + ").\n"
                + "Happy shopping!";
        kafkaTemplate.send("user-registered", message);
    }
}