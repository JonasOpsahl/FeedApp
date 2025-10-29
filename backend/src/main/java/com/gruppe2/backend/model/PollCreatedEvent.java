package com.gruppe2.backend.model;

public class PollCreatedEvent {
    private final Integer pollId;
    private final String question;

    public PollCreatedEvent(Integer pollId, String question) {
        this.pollId = pollId;
        this.question = question;
    }

    public Integer getPollId() {
         return this.pollId;
    }
    public String getQuestion() {
        return this.question; 
    }
}