package com.gruppe2.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="polls")
public class Poll {
    
    private String question;
    private Instant publishedAt;
    private Integer durationDays;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pollId;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    @JsonBackReference
    private User creator;

    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name="poll_invited_users", joinColumns=@JoinColumn(name="poll_id"))
    @Column(name="user_id")
    private List<Integer> invitedUsers;
    private Integer maxVotesPerUser;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<VoteOption> pollOptions = new ArrayList<>();


    public enum Visibility {
        PUBLIC,
        PRIVATE
    }

    public Poll() {

    }

    public VoteOption addVoteOption(String caption) {
        VoteOption newOption = new VoteOption();
        newOption.setCaption(caption);
        newOption.setPresentationOrder(this.pollOptions.size());
        newOption.setPoll(this);
        this.pollOptions.add(newOption);
        return newOption;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    @JsonIgnore
    public Integer getDurationDays() {
        return this.durationDays;
    }

    public void setPollId(Integer pollId) {
        this.pollId = pollId;
    }

    public Integer getPollId() {
        return this.pollId;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public User getCreator() {
        return this.creator;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getValidUntil() {
        return this.validUntil;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @JsonIgnore
    public Visibility getVisibility() {
        return this.visibility;
    }

    public void setInvitedUsers(List<Integer> userIds) {
        this.invitedUsers = userIds;
    }

    @JsonIgnore
    public List<Integer> getInvitedUsers() {
        return this.invitedUsers;
    }

    public Integer getMaxVotesPerUser() {
        return maxVotesPerUser;
    }

    public void setMaxVotesPerUser(Integer maxVotesPerUser) {
        this.maxVotesPerUser = maxVotesPerUser;
    }

    public void setPollOptions(List<VoteOption> pollOptions) { 
        this.pollOptions = pollOptions; 
    }

    @JsonProperty("pollOptions")
    public List<VoteOption> getPollOptions() { 
        return pollOptions; 
    }

    public Integer getCreatorId() {
        return creator != null ? creator.getUserId() : null;
    }

  
}
