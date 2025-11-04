package com.gruppe2.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.gruppe2.backend.model.Poll;
import com.gruppe2.backend.model.User;
import com.gruppe2.backend.model.Vote;
import com.gruppe2.backend.model.VoteOption;
import com.gruppe2.backend.model.Comment;


@Component
@Profile("in-memory")
public class InMemoryPollService implements PollService, CommentService {
    
    private Map<Integer, User> users = new HashMap<>();
    private Map<Integer, Poll> polls = new HashMap<>();
    private Map<Integer, Vote> allVotes = new HashMap<>();
    private final Map<Integer, Comment> comments = new HashMap<>();

    private Integer commentIdCreator() { return comments.size() + 1; }
    


    public InMemoryPollService() {

    }

    private Integer userIdCreator() {
        return users.size()+1;
    }

    private Integer pollIdCreator() {
        return polls.size()+1;
    }

    private Integer voteIdCreator() {
        return allVotes.size()+1;
    }



    // Users

    // Create
    @Override
    public User createUser(String username, String email, String password) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password);
        Integer userId = userIdCreator();
        newUser.setUserId(userId);
        users.put(userId, newUser);
        return newUser;
    }

    // Read
    @Override
    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    // Read
    @Override
    public User getUser(Integer userId) {
        return users.get(userId);
    }

    // Update
    @Override
    public User updateUser(Integer userId, Optional<String> newUsername, Optional<String> newEmail, Optional<String> newPassword) {
        User toChange = getUser(userId);
        if (newUsername.isPresent()) {
            toChange.setUsername(newUsername.get());
        }
        if (newEmail.isPresent()) {
            toChange.setEmail(newEmail.get());
        }
        if (newPassword.isPresent()) {
            toChange.setPassword(newPassword.get());
        }
        users.put(userId, toChange);
        return toChange;
    }

    // Delete
    @Override
    public boolean deleteUser(Integer id) {
        users.remove(id);
        return true;
    }

    // Polls

    // Create
    @Override
    public Poll createPoll(String question, Integer durationDays, Integer creatorid, Poll.Visibility visibility, 
                        Optional<Integer> maxVotesPerUser, 
                        List<Integer> invitedUsers, List<VoteOption> pollOptions) {
        
        User creator = users.get(creatorid);
        if (creator == null) {
            throw new IllegalArgumentException("Creator with ID " + creatorid + " not found.");
        }

        Poll newPoll = new Poll();
        newPoll.setVisibility(visibility);
        newPoll.setQuestion(question);
        newPoll.setPublishedAt(Instant.now());
        newPoll.setDurationDays(durationDays);
        newPoll.setValidUntil(Instant.now().plus(Duration.ofDays(durationDays)));
        newPoll.setMaxVotesPerUser(maxVotesPerUser.orElse(1));
        
        newPoll.setCreator(creator);

        for (VoteOption option : pollOptions) {
            option.setPoll(newPoll);
        }
        newPoll.setPollOptions(pollOptions);

        if (visibility == Poll.Visibility.PRIVATE) {
            invitedUsers.add(creator.getUserId());
            newPoll.setInvitedUsers(invitedUsers.stream().distinct().toList());
        } else {
            newPoll.setInvitedUsers(new ArrayList<>(users.keySet()));
        }
        
        Integer pollId = pollIdCreator();
        newPoll.setPollId(pollId);
        polls.put(pollId, newPoll);

        return newPoll;
    }

    // Read
    @Override
    public List<Poll> getPolls(Optional<Integer> userId) {
        List<Poll> returnList = new ArrayList<>();
        
        if (userId.isEmpty()) {
            for (Poll poll : polls.values()) {
                if (poll.getVisibility() == Poll.Visibility.PUBLIC) {
                    returnList.add(poll);
                }
            }
        } else {
            Integer id = userId.get();
            
            for (Poll poll : polls.values()) {
                if (poll.getVisibility() == Poll.Visibility.PUBLIC || 
                    (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(id))) {
                    returnList.add(poll);
                }
            }
        }
        return returnList;
    }

    // Read
    @Override
    public Poll getPoll(Integer pollId, Integer userId) {
        Poll poll = polls.get(pollId);
        if (poll.getVisibility() == Poll.Visibility.PUBLIC) {
            return polls.get(pollId);
        }
        if (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(userId)) {
            return polls.get(pollId);
        }
        else {
            return null;
        }
    }

    // Update
    @Override
    public Poll updatePoll(Optional<Integer> durationDays, Integer pollId, Integer userId, List<Integer> newInvites) {
        Poll toUpdate = getPoll(pollId, userId);
        
        if (toUpdate == null || !toUpdate.getCreator().getUserId().equals(userId)) {
             return null;
        }


        List<Integer> currentInvites = toUpdate.getInvitedUsers();
        List<Integer> allInvites = Stream.concat(currentInvites.stream(), newInvites.stream())
                                     .distinct()
                                     .collect(Collectors.toList());
        toUpdate.setInvitedUsers(allInvites);


        if (durationDays.isPresent()) {
            Integer durationDaysInt = durationDays.get();
            Instant currentDeadline = toUpdate.getValidUntil();
            Instant newDeadline = currentDeadline.plus(Duration.ofDays(durationDaysInt));
            toUpdate.setValidUntil(newDeadline);

        }
        toUpdate.setPublishedAt(Instant.now());
        polls.put(pollId, toUpdate);

        return toUpdate;
    }

    // Delete
    @Override
    public boolean deletePoll(Integer pollId, Integer userId) {
        Poll poll = polls.get(pollId);
        if (poll != null && poll.getCreator().getUserId().equals(userId)) {
            polls.remove(pollId);
            return true;
        }
        return false;
    }

    // Votes AND VoteOptions

    // Create vote

    @Override
    public boolean castVote(Integer pollId, Optional<Integer> userId, Integer presentationOrder) {
        Poll poll = polls.get(pollId);

        if (poll == null || Instant.now().isAfter(poll.getValidUntil())) {
            return false;
        }

        VoteOption chosenOption = null;
        for (VoteOption option : poll.getPollOptions()) {
            if (option.getPresentationOrder() == presentationOrder) {
                chosenOption = option;
                break;
            }
        }

        if (chosenOption == null) {
            return false; 
        }
        User voter = null; 
        if (userId.isPresent()) {
            voter = users.get(userId.get());
            if(voter == null) return false; 
        }


        // VOTING CHECKS HERE
        if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
            if (voter == null || !poll.getInvitedUsers().contains(voter.getUserId())) {
                return false;
        }

            int userVoteCount = 0;
            for (Vote vote : allVotes.values()) {
                if (vote.getVoter().equals(voter) && poll.getPollOptions().contains(vote.getChosenOption())) {
                    userVoteCount++;
                }
            }

            if (userVoteCount >= poll.getMaxVotesPerUser()) {
                return false;
            }
        }

        Vote newVote = new Vote();
        newVote.setVoteId(voteIdCreator());
        newVote.setPublishedAt(Instant.now());
        newVote.setVoter(voter);

        newVote.setChosenOption(chosenOption);
        
        allVotes.put(newVote.getVoteId(), newVote);
        return true;
    }

    @Override
    public Map<String, Integer> getPollResults(Integer pollId) {
        Poll poll = polls.get(pollId);
        if (poll == null) {
            return null;
        }
        Map<String, Integer> results = new HashMap<>();
        for (VoteOption option : poll.getPollOptions()) {
            results.put(option.getCaption(), 0);
        }
        for (Vote vote : allVotes.values()) {
            if (poll.getPollOptions().contains(vote.getChosenOption())) {
                String caption = vote.getChosenOption().getCaption();
                results.put(caption, results.get(caption) + 1);
            }
        }
        return results;
    }

    // For testing
    public void reset() {
        users.clear();
        polls.clear();
        allVotes.clear();
    }

    // Not used here but needed from interface
    @Override
    public void loginUser(Integer userId) {
    }

    @Override
    public void logoutUser(Integer userId) {
    }

    @Override
    public boolean isUserLoggedIn(Integer userId) {
        return false;
    }

    @Override
    public Set<String> getLoggedInUsers() {
        return new HashSet<>();
    }

    @Override
    public void invalidatePollCache(Integer pollId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'invalidatePollCache'");
    }

    @Override
    public Poll addOptionsToPoll(Integer pollId, Integer userId, List<VoteOption> newOptions) {
        Poll poll = polls.get(pollId);
        if (poll == null || poll.getCreator() == null || !poll.getCreator().getUserId().equals(userId)) {
            return null;
        }
        int maxOrder = poll.getPollOptions().stream()
            .map(VoteOption::getPresentationOrder)
            .max(Comparator.naturalOrder())
            .orElse(0);

        for (VoteOption vo : newOptions) {
            if (vo.getCaption() == null || vo.getCaption().isBlank()) continue;
            if (vo.getPresentationOrder() == null || vo.getPresentationOrder() <= 0) {
                maxOrder += 1;
                vo.setPresentationOrder(maxOrder);
            }
            vo.setPoll(poll);
            poll.getPollOptions().add(vo);
        }
        return poll;
    }
    

    @Override
    public Comment addComment(Integer pollId, Integer authorId, String content, Optional<Integer> parentId) {
        Poll poll = polls.get(pollId);
        if (poll == null) throw new IllegalArgumentException("poll not found: " + pollId);
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("content empty");

        Comment c = new Comment();
        c.setCommentId(commentIdCreator());
        c.setPoll(poll);
        c.setAuthorId(authorId);
        c.setContent(content.trim());
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        parentId.ifPresent(pid -> {
            Comment p = comments.get(pid);
            if (p != null) c.setParent(p);
        });
        comments.put(c.getCommentId(), c);
        return c;
    }

    @Override
    public List<Comment> getTopLevel(Integer pollId, int offset, int limit) {
        return comments.values().stream()
            .filter(c -> c.getPoll() != null && c.getPoll().getPollId().equals(pollId) && c.getParent() == null)
            .sorted((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .skip(Math.max(0, offset))
            .limit(Math.max(1, limit))
            .toList();
    }

     @Override
    public long countTopLevel(Integer pollId) {
        return comments.values().stream()
            .filter(c -> c.getPoll() != null && c.getPoll().getPollId().equals(pollId) && c.getParent() == null)
            .count();
    }

    @Override
    public List<Comment> getReplies(Integer pollId, Integer parentId, int offset, int limit) {
        return comments.values().stream()
            .filter(c -> c.getPoll() != null && c.getPoll().getPollId().equals(pollId)
                       && c.getParent() != null && c.getParent().getCommentId().equals(parentId))
            .sorted((a,b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .skip(Math.max(0, offset))
            .limit(Math.max(1, limit))
            .toList();
    }

    @Override
    public long countReplies(Integer pollId, Integer parentId) {
        return comments.values().stream()
            .filter(c -> c.getPoll() != null && c.getPoll().getPollId().equals(pollId)
                       && c.getParent() != null && c.getParent().getCommentId().equals(parentId))
            .count();
    }

     @Override
    public Optional<Comment> findById(Integer commentId) {
        return Optional.ofNullable(comments.get(commentId));
    }

    @Override
    public Comment updateContent(Integer commentId, String newContent) {
        Comment c = comments.get(commentId);
        if (c == null) throw new IllegalArgumentException("comment not found");
        c.setContent(Objects.requireNonNullElse(newContent, "").trim());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    @Override
    public void delete(Integer commentId) {
    if (commentId == null) return;
    // collect subtree
    List<Integer> toRemove = new ArrayList<>();
    collect(commentId, toRemove);
    for (Integer id : toRemove) comments.remove(id);
}
private void collect(Integer id, List<Integer> acc) {
    acc.add(id);
    comments.values().stream()
        .filter(c -> c.getParent() != null && id.equals(c.getParent().getCommentId()))
        .map(Comment::getCommentId)
        .forEach(childId -> collect(childId, acc));
}

}
