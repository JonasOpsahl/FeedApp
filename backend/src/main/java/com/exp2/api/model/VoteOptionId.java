package com.exp2.api.model;

import java.io.Serializable;
import java.util.Objects;

public class VoteOptionId implements Serializable {

    private Integer poll;
    private Integer presentationOrder;

    public VoteOptionId() {
    }

    public VoteOptionId(Integer poll, Integer presentationOrder) {
        this.poll = poll;
        this.presentationOrder = presentationOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoteOptionId that = (VoteOptionId) o;
        return Objects.equals(poll, that.poll) && Objects.equals(presentationOrder, that.presentationOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poll, presentationOrder);
    }
}