package com.gruppe2.backend.controller;

import org.springframework.web.bind.annotation.RestController;

import com.gruppe2.backend.model.Poll;
import com.gruppe2.backend.model.VoteOption;
import com.gruppe2.backend.service.PollService;
import com.gruppe2.backend.service.ProducerService;

import java.util.ArrayList;
import java.util.List;
import com.gruppe2.backend.config.RawWebSocketServer;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@CrossOrigin
@RequestMapping("/api/polls")
public class PollController {
    
    private PollService pollService;
    private ProducerService producerService;

    public PollController(PollService pollService, ProducerService producerService) {
        this.pollService = pollService;
        this.producerService = producerService; 
    }

    @RequestMapping
    public List<Poll> getPolls(@RequestParam(required = false) Optional<Integer> userId) {
        return pollService.getPolls(userId);
    }
    @RequestMapping("/{id}")
    public Poll getPoll(@PathVariable Integer id, @RequestParam Integer userId) {
        return pollService.getPoll(id, userId);
    }

    @PostMapping
    public Poll createPoll(@RequestParam String question,
                           @RequestParam Integer durationDays,
                           @RequestParam Integer creatorId,
                           @RequestParam Poll.Visibility visibility,
                           @RequestParam Optional<Integer> maxVotesPerUser,
                           @RequestParam(required = false) List<Integer> invitedUsers,
                           @RequestParam List<String> optionCaptions,
                           @RequestParam List<Integer> optionOrders) {

        List<VoteOption> pollOptions = new ArrayList<>();
        for (int i = 0; i < optionCaptions.size(); i++) {
            VoteOption option = new VoteOption();
            option.setCaption(optionCaptions.get(i));
            option.setPresentationOrder(optionOrders.get(i));
            pollOptions.add(option);
        }

         Poll created = pollService.createPoll(
        question, durationDays, creatorId, visibility,
        maxVotesPerUser,
        invitedUsers == null ? new ArrayList<>() : invitedUsers,
        pollOptions
        );

        // WebSocket notifications
        RawWebSocketServer.broadcast("pollsUpdated");
        RawWebSocketServer.broadcastJson(Map.of(
            "type", "poll-created",
            "pollId", created.getPollId(),
            "ts", System.currentTimeMillis()
        ));

        return created;
    }

    @PostMapping("/{id}/vote")
    @ResponseStatus(HttpStatus.OK)
    public void castVote(@PathVariable("id") Integer pollId,
                        @RequestParam Integer presentationOrder,
                        @RequestParam(required = false) Optional<Integer> userId) {

        boolean success = pollService.castVote(pollId, userId, presentationOrder);

        if (success) {
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "vote-delta",
                "pollId", pollId,
                "optionOrder", presentationOrder
            ));
        }
    }

    //Test
    @GetMapping("/{id}/results")
    public Map<String, Integer> getPollResults(@PathVariable("id") Integer pollId) {
        return pollService.getPollResults(pollId);

        // WebSocket: optimistic vote delta + legacy ping
        RawWebSocketServer.broadcast("votesUpdated");
        RawWebSocketServer.broadcastJson(Map.of(
            "type", "vote-delta",
            "pollId", pollId,
            "optionOrder", presentationOrder,
            "voterUserId", userId.orElse(null),
            "ts", System.currentTimeMillis()
        ));
    }

    @PutMapping("/{id}")
    public Poll updatePoll(@PathVariable Integer id, @RequestParam Optional<Integer> durationDays, @RequestParam Integer userId, @RequestParam(required = false) List<Integer> newInvites) {
        if (newInvites == null) {
            List<Integer> newInvitesEmpty = new ArrayList<>();
            return pollService.updatePoll(durationDays, id ,userId, newInvitesEmpty);
        }
        return pollService.updatePoll(durationDays, id ,userId, newInvites);
    }

    @DeleteMapping("/{id}")
    public boolean deletePoll(@PathVariable Integer id,
                              @RequestParam Integer userId) {
        boolean ok = pollService.deletePoll(id, userId);  // pass owner id
        if (ok) {
            RawWebSocketServer.broadcast("pollsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "poll-deleted",
                "pollId", id,
                "ts", System.currentTimeMillis()
            ));
        }
        return ok;
    }


    @PostMapping("/{id}/options")
    public Poll addOptionsToPoll(@PathVariable Integer id,
                             @RequestParam Integer userId,
                             @RequestParam List<String> optionCaptions,
                             @RequestParam List<Integer> optionOrders) {
    List<VoteOption> pollOptions = new ArrayList<>();
    for (int i = 0; i < optionCaptions.size(); i++) {
        VoteOption option = new VoteOption();
        option.setCaption(optionCaptions.get(i));
        option.setPresentationOrder(optionOrders.get(i));
        pollOptions.add(option);
    }

    Poll updated = pollService.addOptionsToPoll(id, userId, pollOptions);

    if (updated != null) {
        RawWebSocketServer.broadcast("pollsUpdated");
        RawWebSocketServer.broadcastJson(Map.of(
            "type", "poll-updated",
            "pollId", id,
            "ts", System.currentTimeMillis()
        ));
    }
    return updated;
}
    

}
