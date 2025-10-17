package com.gruppe2.FeedApp.Controllers;


import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.web.bind.annotation.*;

import com.gruppe2.FeedApp.Domain.Vote;
import com.gruppe2.FeedApp.Services.PollManager;
import com.gruppe2.FeedApp.WebSocketServer;

import java.util.List;
import java.util.Locale;


@CrossOrigin
@RestController
@RequestMapping("/api/polls/{pollId}/votes")
public class VoteController {
    private final PollManager manager;
    public VoteController(PollManager manager) {
        this.manager = manager;
    }

    @PostMapping
    public Vote cast(@PathVariable Long pollId, @RequestBody VoteRequest req) {
    Vote vote = manager.castOrChangeVote(pollId, req.optionId, req.voterUserId, req.anonymous, req.isUpvote);
        Vote persisted = manager.findVote(vote.getId()).orElse(vote);
        String payload = String.format(Locale.ROOT,
                "{\"type\":\"vote-delta\",\"pollId\":%d,\"optionId\":%d,\"voteId\":%d,\"upvote\":%s,\"voterUserId\":%s,\"ts\":%d}",
                persisted.getPollId(),
                persisted.getOptionId(),
                persisted.getId(),
                persisted.isUpvote(),
                persisted.getVoterUserId() == null ? "null" : persisted.getVoterUserId().toString(),
                System.currentTimeMillis());
    WebSocketServer.broadcast(payload);
        return persisted;
    }

    @GetMapping
    public List<Vote> listAllVotes(@PathVariable Long pollId) {
        return manager.votesForPoll(pollId);
    }

    @GetMapping("/{voteId}")
    public Vote get(@PathVariable Long pollId, @PathVariable Long voteId) {
        return manager.findVote(voteId).orElseThrow();
    }

    @PutMapping("/{voteId}")
    public Vote update(@PathVariable Long pollId, @PathVariable Long voteId, @RequestBody VoteRequest req) {
        Vote vote = manager.castOrChangeVote(pollId, req.optionId, req.voterUserId, req.anonymous, req.isUpvote);
        return manager.findVote(vote.getId()).orElse(vote);
    }

    @DeleteMapping("/{voteId}")
    public void delete(@PathVariable Long pollId, @PathVariable Long voteId) {
    }

    public static class VoteRequest {
    public Long optionId;
        public Long voterUserId; 
        public boolean anonymous;
        public boolean isUpvote;
    }
}
