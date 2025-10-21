package com.gruppe2.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="vote_options")
@IdClass(VoteOptionId.class)
public class VoteOption {
    
    @Id
    @ManyToOne
    @JoinColumn(name="poll_id")
    @JsonBackReference
    private Poll poll;
    private String caption;

    @Id
    private Integer presentationOrder;

    public VoteOption() {

    }

    public Poll getPoll(){
        return this.poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return this.caption;
    }

    public Integer getPresentationOrder() {
        return this.presentationOrder;
    }

    public void setPresentationOrder(Integer presentationOrder) {
        this.presentationOrder = presentationOrder;
    }

}
