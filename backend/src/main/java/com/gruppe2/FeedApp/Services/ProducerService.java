package com.gruppe2.FeedApp.Services;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.logging.Logger;


@Service
public class ProducerService {
    
    private KafkaTemplate<String, Object> kafkaTemplate;
    private PollTopicManager pollTopicManager;
    private static final Logger logger = Logger.getLogger(ProducerService.class.getName());

    public ProducerService(KafkaTemplate<String, Object> kafkaTemplate, PollTopicManager pollTopicManager) {
        this.kafkaTemplate = kafkaTemplate;
        this.pollTopicManager = pollTopicManager;
    }

    public void sendEvent(Map<String, Object> voteData) {
        Integer pollId = (Integer) voteData.get("pollId");
        String topicName = pollTopicManager.getTopicNameForPoll(pollId);
        
        logger.info("Sending event to topic" + topicName);
        kafkaTemplate.send(topicName, voteData);



    }
}
