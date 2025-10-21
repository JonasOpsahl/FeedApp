package com.gruppe2.backend.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "votes")
public class Vote {
    
    private Instant publishedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer voteId;

    @ManyToOne
    @JoinColumn(name = "voter_id")
    private User voter;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "option_poll_id", referencedColumnName = "poll_id"),
        @JoinColumn(name = "option_presentation_order", referencedColumnName = "presentationOrder")
    })
    private VoteOption chosenOption;
    
    private Integer userId;

    
    public Vote() {

    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public Integer getVoteId() {
        return this.voteId;
    }
    
    public void setVoterId(Integer userId) {
        this.userId = userId;
    }

    public Integer getVoterId() {
        return this.userId;
    }

    public void setVoter(User voter) {
        this.voter = voter;
    }

    public User getVoter() {
        return this.voter;
    }

    public void setChosenOption(VoteOption chosenOption) {
        this.chosenOption = chosenOption;
    }

    public VoteOption getChosenOption() {
        return this.chosenOption;
    }

}
