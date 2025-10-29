package com.gruppe2.backend.service;

import com.gruppe2.backend.model.PollCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.HashMap;
import java.util.Map;

@Component
public class PollEventListener {

    private final ProducerService producerService;
    private final PollTopicManager pollTopicManager;

    public PollEventListener(ProducerService producerService, PollTopicManager pollTopicManager) {
        this.producerService = producerService;
        this.pollTopicManager = pollTopicManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePollCreatedEvent(PollCreatedEvent event) {
        System.out.println("Poll created, preparing Kafka event for pollId: " + event.getPollId());
        
        Map<String, Object> pollData = new HashMap<>();
        pollData.put("pollId", event.getPollId());
        pollData.put("question", event.getQuestion());
        pollData.put("eventType", "POLL_CREATED");

        String topicName = pollTopicManager.getTopicNameForPoll(event.getPollId()) + ".events";
        producerService.sendEvent(topicName, pollData);
    }
}