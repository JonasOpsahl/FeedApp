package com.gruppe2.FeedApp.service;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class PollTopicManager {
    
    public String POLL_EVENTS_TOPIC_PREFIX = "poll.voteChange.";
    private KafkaAdmin kafkaAdmin;
    private static final Logger logger = Logger.getLogger(PollTopicManager.class.getName());

    public PollTopicManager(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    public String getTopicNameForPoll(Integer pollId) {
        return POLL_EVENTS_TOPIC_PREFIX + pollId;
    }

    public void createPollTopic(Integer pollId) {
        String topicName = getTopicNameForPoll(pollId);
        NewTopic topic = new NewTopic(topicName, 1, (short) 1);
        kafkaAdmin.createOrModifyTopics(topic);
        logger.info("Topic created successfully.");
    }
}
