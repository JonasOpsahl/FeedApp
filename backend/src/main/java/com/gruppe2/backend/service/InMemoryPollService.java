package com.gruppe2.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

@Component
@Profile("in-memory")
public class InMemoryPollService implements PollService{
    
    private Map<Integer, User> users = new HashMap<>();
    private Map<Integer, Poll> polls = new HashMap<>();
    private Map<Integer, Vote> allVotes = new HashMap<>();

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
    public boolean deletePoll(Integer pollId) {
        polls.remove(pollId);
        return true;
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

}
