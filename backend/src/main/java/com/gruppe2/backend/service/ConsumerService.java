package com.gruppe2.backend.service;

import java.util.Map;
import java.util.logging.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.gruppe2.backend.config.RawWebSocketServer;

@Service
public class ConsumerService {
    private final PollService pollService;
    private static final Logger logger = Logger.getLogger(ConsumerService.class.getName());

    public ConsumerService(PollService pollService) {
        this.pollService = pollService;
    }

    @KafkaListener(topicPattern = "poll.voteChange.*", groupId = "poll-app")
    public void consumeVoteChangeEvent(Map<String, Object> voteData) {
        Integer pollId = (Integer) voteData.get("pollId");
        Integer optionOrder = (Integer) voteData.get("optionOrder");
        if (pollId != null) {
            logger.info("Kafka consumer received event for poll " + pollId + ". Invalidating cache.");
            pollService.invalidatePollCache(pollId);

            if (optionOrder != null) {
                RawWebSocketServer.broadcastJson(Map.of(
                    "type", "vote-delta",
                    "pollId", pollId,
                    "optionOrder", optionOrder,
                    "ts", System.currentTimeMillis()
                ));
            }
        }
    }
}
