package com.gruppe2.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gruppe2.backend.config.RawWebSocketServer;
import com.gruppe2.backend.model.Comment;
import com.gruppe2.backend.model.Poll;
import com.gruppe2.backend.model.User;
import com.gruppe2.backend.model.VoteOption;
import com.gruppe2.backend.service.CommentService;
import com.gruppe2.backend.service.PollService;
import com.gruppe2.backend.service.ProducerService;

@RestController
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
    public List<Poll> getPolls(@AuthenticationPrincipal User authenticatedUser) {
        Optional<Integer> userId = (authenticatedUser != null)
            ? Optional.of(authenticatedUser.getUserId())
            : Optional.empty();
        return pollService.getPolls(userId);
    }

    @RequestMapping("/{id}")
    public Poll getPoll(@PathVariable Integer id, @AuthenticationPrincipal User authenticatedUser) {
        Integer userId = (authenticatedUser != null) ? authenticatedUser.getUserId() : null;
        return pollService.getPoll(id, userId);
    }

    @PostMapping
    public Poll createPoll(@AuthenticationPrincipal User authenticatedUser,
                           @RequestParam String question,
                           @RequestParam Integer durationDays,
                           @RequestParam Poll.Visibility visibility,
                           @RequestParam Optional<Integer> maxVotesPerUser,
                           @RequestParam(required = false) List<Integer> invitedUsers,
                           @RequestParam List<String> optionCaptions,
                           @RequestParam List<Integer> optionOrders) {

        Integer creatorId = authenticatedUser.getUserId();

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
    public boolean castVote(
        @PathVariable("id") Integer pollId,
        @RequestParam Integer presentationOrder,
        @AuthenticationPrincipal User authenticatedUser) {

        Optional<Integer> userId = (authenticatedUser != null)
            ? Optional.of(authenticatedUser.getUserId())
            : Optional.empty();

        boolean success = pollService.castVote(pollId, userId, presentationOrder);

        return success;
    }

    @GetMapping("/{id}/results")
    public Map<String, Integer> getPollResults(@PathVariable("id") Integer pollId) {
        return pollService.getPollResults(pollId);
    }

    @PutMapping("/{id}")
    public Poll updatePoll(@PathVariable Integer id,
                           @RequestParam Optional<Integer> durationDays,
                           @AuthenticationPrincipal User authenticatedUser,
                           @RequestParam(required = false) List<Integer> newInvites) {

        Integer userId = authenticatedUser.getUserId();

        List<Integer> invitesList = (newInvites == null) ? new ArrayList<>() : newInvites;

        return pollService.updatePoll(durationDays, id, userId, invitesList);
    }

    @DeleteMapping("/{id}")
    public boolean deletePoll(@PathVariable Integer id,
                              @AuthenticationPrincipal User authenticatedUser) {

        Integer userId = authenticatedUser.getUserId();
        boolean ok = pollService.deletePoll(id, userId);

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
                                 @AuthenticationPrincipal User authenticatedUser,
                                 @RequestParam List<String> optionCaptions,
                                 @RequestParam List<Integer> optionOrders) {

        Integer userId = authenticatedUser.getUserId();

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
        public String content;
        public Integer parentId;
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Integer id,
                                        @AuthenticationPrincipal User authenticatedUser,
                                        @RequestBody AddCommentRequest body) {
        try {
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in to comment.");
            }
            if (body == null || body.content == null) {
                return ResponseEntity.badRequest().body("content is required");
            }

            Integer authorId = authenticatedUser.getUserId();

            var created = commentService.addComment(id, authorId, body.content,
                    Optional.ofNullable(body.parentId));

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
        public String content;
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<?> editComment(@PathVariable Integer id,
                                         @PathVariable Integer commentId,
                                         @AuthenticationPrincipal User authenticatedUser,
                                         @RequestBody EditCommentRequest body) {
        try {
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in to edit.");
            }
            if (body == null || body.content == null) {
                return ResponseEntity.badRequest().body("content is required");
            }

            var updated = commentService.updateContent(commentId, body.content, authenticatedUser.getUserId());

            // broadcast: comment-updated
            RawWebSocketServer.broadcast("commentsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "comment-updated",
                "pollId", id,
                "commentId", commentId,
                "ts", System.currentTimeMillis()
            ));

            return ResponseEntity.ok(updated);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer id,
                                           @PathVariable Integer commentId,
            @AuthenticationPrincipal User authenticatedUser) {
        try {
            if (authenticatedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in to delete.");
            }

            commentService.delete(commentId, authenticatedUser.getUserId());

            // broadcast: comment-deleted
            RawWebSocketServer.broadcast("commentsUpdated");
            RawWebSocketServer.broadcastJson(Map.of(
                "type", "comment-deleted",
                "pollId", id,
                "commentId", commentId,
                "ts", System.currentTimeMillis()
            ));

            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
