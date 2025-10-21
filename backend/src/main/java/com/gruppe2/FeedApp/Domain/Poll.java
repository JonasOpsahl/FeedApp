package com.gruppe2.FeedApp.domain;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

 
@Entity
@Table(name = "polls")
public class Poll implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;
    @Column(nullable = false)
    private String question;
    private Instant publishedAt;
    private Instant validUntil;
    @Column(name = "public_poll")
    private boolean publicPoll;
    private Integer maxVotesPerUser;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "poll_invited_usernames", joinColumns = @JoinColumn(name = "poll_id"))
    @Column(name = "username")
    private Set<String> invitedUsernames = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    @JsonIgnore
    private User createdBy;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<VoteOption> options = new ArrayList<>();

    
    public VoteOption addVoteOption(String caption) {
        VoteOption option = new VoteOption();
        option.setCaption(caption);
        option.setPoll(this);
        option.setPresentationOrder(this.options.size());
        this.options.add(option);
        return option;
    }

    
    public Poll() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }

    public boolean isPublicPoll() { return publicPoll; }
    public void setPublicPoll(boolean publicPoll) { this.publicPoll = publicPoll; }

    public Integer getMaxVotesPerUser() { return maxVotesPerUser; }
    public void setMaxVotesPerUser(Integer maxVotesPerUser) { this.maxVotesPerUser = maxVotesPerUser; }

    public Set<String> getInvitedUsernames() { return invitedUsernames; }
    public void setInvitedUsernames(Set<String> invitedUsernames) { this.invitedUsernames = invitedUsernames; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    @JsonProperty("creatorUserId")
    public Long getCreatorUserId() {
        return createdBy != null ? createdBy.getId() : null;
    }

    public List<VoteOption> getOptions() { return options; }
    public void setOptions(List<VoteOption> options) { this.options = options; }

}

