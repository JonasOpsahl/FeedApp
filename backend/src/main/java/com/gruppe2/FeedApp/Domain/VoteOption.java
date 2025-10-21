package com.gruppe2.FeedApp.domain;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;
import java.util.Objects;


 
@Entity
@Table(name = "vote_options")
public class VoteOption implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    @JsonBackReference
    private Poll poll;

    private String caption;
    private int presentationOrder;


    public VoteOption() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public int getPresentationOrder() { return presentationOrder; }
    public void setPresentationOrder(int presentationOrder) { this.presentationOrder = presentationOrder; }


    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoteOption)) return false;
        VoteOption that = (VoteOption) o;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
