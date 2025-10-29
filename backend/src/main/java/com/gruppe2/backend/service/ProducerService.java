package com.gruppe2.backend.service;

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

    public void sendEvent(String topicName, Map<String, Object> data) {
        logger.info("Sending event to topic " + topicName);
        kafkaTemplate.send(topicName, data);
    }
}
