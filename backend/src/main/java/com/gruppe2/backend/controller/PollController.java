package com.gruppe2.backend.controller;

import org.springframework.web.bind.annotation.RestController;

import com.gruppe2.backend.model.Poll;
import com.gruppe2.backend.model.VoteOption;
import com.gruppe2.backend.service.CommentService;
import com.gruppe2.backend.service.PollService;
import com.gruppe2.backend.service.ProducerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.gruppe2.backend.config.RawWebSocketServer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.gruppe2.backend.model.Comment;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@CrossOrigin
@RequestMapping("/api/polls")
public class PollController {

    private PollService pollService;
    private ProducerService producerService;
    private final CommentService commentService;

    public PollController(PollService pollService, ProducerService producerService, CommentService commentService) {
        this.pollService = pollService;
        this.producerService = producerService;
        this.commentService = commentService;
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
                pollOptions);

        RawWebSocketServer.broadcast("pollsUpdated");
        RawWebSocketServer.broadcastJson(Map.of(
                "type", "poll-created",
                "pollId", created.getPollId(),
                "ts", System.currentTimeMillis()));

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
                    "optionOrder", presentationOrder));
        }
    }

    @GetMapping("/{id}/results")
    public Map<String, Integer> getPollResults(@PathVariable("id") Integer pollId) {
        return pollService.getPollResults(pollId);
    }

    @PutMapping("/{id}")
    public Poll updatePoll(@PathVariable Integer id, @RequestParam Optional<Integer> durationDays,
            @RequestParam Integer userId, @RequestParam(required = false) List<Integer> newInvites) {
        if (newInvites == null) {
            List<Integer> newInvitesEmpty = new ArrayList<>();
            return pollService.updatePoll(durationDays, id, userId, newInvitesEmpty);
        }
        return pollService.updatePoll(durationDays, id, userId, newInvites);
    }

    @DeleteMapping("/{id}")
    public boolean deletePoll(@PathVariable Integer id,
            @RequestParam Integer userId) {
        boolean ok = pollService.deletePoll(id, userId); // pass owner id
        if (ok) {
            RawWebSocketServer.broadcast("pollsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                    "type", "poll-deleted",
                    "pollId", id,
                    "ts", System.currentTimeMillis()));
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
                    "ts", System.currentTimeMillis()));
        }
        return updated;
    }

    public static class CommentsPage<T> {
        public List<T> items;
        public long total;
        public boolean hasMore;
        public int nextOffset;

        public CommentsPage(List<T> items, long total, int offset, int limit) {
            this.items = items;
            this.total = total;
            int consumed = offset + items.size();
            this.hasMore = consumed < total;
            this.nextOffset = consumed;
        }
    }
    

    @GetMapping("/{id}/comments")
    public CommentsPage<Comment> getTopLevel(@PathVariable Integer id,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "5") Integer limit) {
        var items = commentService.getTopLevel(id, offset, limit);
        var total = commentService.countTopLevel(id);
        return new CommentsPage<>(items, total, offset, limit);
    }

    @GetMapping("/{id}/comments/replies")
    public CommentsPage<Comment> getReplies(@PathVariable Integer id,
            @RequestParam Integer parentId,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "3") Integer limit) {
        var items = commentService.getReplies(id, parentId, offset, limit);
        var total = commentService.countReplies(id, parentId);
        return new CommentsPage<>(items, total, offset, limit);
    }

    public static class AddCommentRequest {
        public Integer authorId;
        public String content;
        public Integer parentId;
    }

  @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Integer id, @RequestBody AddCommentRequest body) {
        try {
            if (body == null || body.authorId == null || body.content == null) {
                return ResponseEntity.badRequest().body("authorId and content are required");
            }
            var created = commentService.addComment(id, body.authorId, body.content,
                    Optional.ofNullable(body.parentId));

            // broadcast: comment-created
            RawWebSocketServer.broadcast("commentsUpdated");
            var payload = new HashMap<String, Object>();
            payload.put("type", "comment-created");
            payload.put("pollId", id);
            payload.put("commentId", created.getCommentId());
            if (created.getParent() != null) {
                payload.put("parentId", created.getParent().getCommentId());
            }
            payload.put("ts", System.currentTimeMillis());
            RawWebSocketServer.broadcastJson(payload);

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

     public static class EditCommentRequest {
        public Integer editorId;
        public String content;
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<?> editComment(@PathVariable Integer id,
                                         @PathVariable Integer commentId,
                                         @RequestBody EditCommentRequest body) {
        try {
            var cOpt = commentService.findById(commentId);
            if (cOpt.isEmpty() || !Objects.equals(cOpt.get().getPoll().getPollId(), id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("comment not found");
            }
            var c = cOpt.get();
            if (body == null || body.editorId == null || body.content == null) {
                return ResponseEntity.badRequest().body("editorId and content required");
            }
            if (!Objects.equals(c.getAuthorId(), body.editorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only author can edit");
            }
            var updated = commentService.updateContent(commentId, body.content);

            // broadcast: comment-updated
            RawWebSocketServer.broadcast("commentsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "comment-updated",
                "pollId", id,
                "commentId", commentId,
                "ts", System.currentTimeMillis()
            ));

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer id,
                                           @PathVariable Integer commentId,
                                           @RequestParam Integer requesterId) {
        try {
            var cOpt = commentService.findById(commentId);
            if (cOpt.isEmpty() || !Objects.equals(cOpt.get().getPoll().getPollId(), id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("comment not found");
            }
            var c = cOpt.get();
            Integer ownerId = c.getPoll().getCreator() != null ? c.getPoll().getCreator().getUserId() : null;
            if (!Objects.equals(c.getAuthorId(), requesterId) && !Objects.equals(ownerId, requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not allowed");
            }
            commentService.delete(commentId);

            // broadcast: comment-deleted
            RawWebSocketServer.broadcast("commentsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "comment-deleted",
                "pollId", id,
                "commentId", commentId,
                "ts", System.currentTimeMillis()
            ));

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
