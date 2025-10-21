package com.exp2.api.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;


@Service
public class ConsumerService {
    private PollService pollService;
    private static final Logger logger = Logger.getLogger(ConsumerService.class.getName());

    public ConsumerService(PollService pollService) {
        this.pollService = pollService;
    }

    @KafkaListener(topicPattern = "poll.voteChange.*", groupId = "poll-app")
    public void consumeEvent(Map<String, Object> voteData) {
        Integer pollId = (Integer) voteData.get("pollId");
        Integer presentationOrder = (Integer) voteData.get("presentationOrder");
        Integer userId = (Integer) voteData.get("userId");

        logger.info("Consuming change for pollId" +pollId);
        pollService.castVote(pollId, Optional.ofNullable(userId), presentationOrder);

    }

}
