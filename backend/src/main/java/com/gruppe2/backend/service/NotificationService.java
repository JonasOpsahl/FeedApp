package com.gruppe2.backend.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class NotificationService {

    @KafkaListener(topicPattern = "poll\\.voteChange\\..*\\.events", groupId = "notification-group")
    public void handlePollEvents(Map<String, Object> event) {
        if ("POLL_CREATED".equals(event.get("eventType"))) {
            Integer pollId = (Integer) event.get("pollId");
            String question = (String) event.get("question");

            System.out.println("--- NOTIFICATION SERVICE ---");
            System.out.println("Received POLL_CREATED event for poll ID: " + pollId);
            System.out.println("Question: '" + question + "'");
            System.out.println("--------------------------");
        }
    }
}