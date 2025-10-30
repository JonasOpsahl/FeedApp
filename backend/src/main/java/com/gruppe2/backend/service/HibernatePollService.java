package com.gruppe2.backend.service;

import com.gruppe2.backend.model.*;
import jakarta.persistence.EntityManager;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("hibernatePollService")
@Profile("database")
public class HibernatePollService implements PollService {

    private EntityManager em;

    private final PollTopicManager pollTopicManager;
    private final ProducerService producerService;

    public HibernatePollService(EntityManager em, PollTopicManager pollTopicManager, ProducerService producerService) {
        this.em = em;
        this.pollTopicManager = pollTopicManager;
        this.producerService = producerService;
    }

    // Users

    @Override
    @Transactional
    public User createUser(String username, String email, String password) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password);
        em.persist(newUser);
        em.flush();
        return newUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Integer userId) {
        return em.find(User.class, userId);
    }

    @Override
    @Transactional
    public User updateUser(Integer userId, Optional<String> newUsername, Optional<String> newEmail, Optional<String> newPassword) {
        User toChange = em.find(User.class, userId);
        if (toChange != null) {
            newUsername.ifPresent(toChange::setUsername);
            newEmail.ifPresent(toChange::setEmail);
            newPassword.ifPresent(toChange::setPassword);
        }
        return toChange;
    }

    @Override
    @Transactional
    public boolean deleteUser(Integer id) {
        User toDelete = em.find(User.class, id);
        if (toDelete != null) {
            em.remove(toDelete);
            return true;
        }
        return false;
    }

    // Polls

    @Override
    @Transactional
    public Poll createPoll(String question, Integer durationDays, Integer creatorId,
                        Poll.Visibility visibility,
                        Optional<Integer> maxVotesPerUser,
                        List<Integer> invitedUsers, List<VoteOption> pollOptions) {
        
        User creator = em.find(User.class, creatorId);
        if (creator == null) {
            throw new IllegalArgumentException("User with ID " + creatorId + " does not exist.");
        }

        Poll newPoll = new Poll();
        newPoll.setQuestion(question);
        newPoll.setCreator(creator);
        newPoll.setVisibility(visibility);
        newPoll.setPublishedAt(Instant.now());
        newPoll.setMaxVotesPerUser(maxVotesPerUser.orElse(1));
        newPoll.setDurationDays(durationDays);
        newPoll.setValidUntil(Instant.now().plus(Duration.ofDays(durationDays)));

        if (visibility == Poll.Visibility.PRIVATE) {
            invitedUsers.add(creator.getUserId());
            newPoll.setInvitedUsers(invitedUsers.stream().distinct().toList());
        } else {
            newPoll.setInvitedUsers(new ArrayList<>());
        }

        for (VoteOption option : pollOptions) {
            option.setPoll(newPoll);
        }
        newPoll.setPollOptions(pollOptions);

        em.persist(newPoll);
        em.flush();

        return newPoll;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Poll> getPolls(Optional<Integer> userId) {
        List<Poll> allPolls = em.createQuery("SELECT p FROM Poll p", Poll.class).getResultList();

        Stream<Poll> visiblePollsStream;
        if (userId.isEmpty()) {
            visiblePollsStream = allPolls.stream()
                .filter(poll -> poll.getVisibility() == Poll.Visibility.PUBLIC);
        } else {
            Integer id = userId.get();
            visiblePollsStream = allPolls.stream()
                .filter(poll ->
                    poll.getVisibility() == Poll.Visibility.PUBLIC ||
                    poll.getCreator().getUserId().equals(id) ||
                    (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(id))
                );
        }

        List<Poll> visiblePolls = visiblePollsStream.collect(Collectors.toList());
        visiblePolls.forEach(poll -> poll.getPollOptions().size());
        return visiblePolls;
    }

    @Override
    @Transactional(readOnly = true)
    public Poll getPoll(Integer pollId, Integer userId) {
        Poll poll = em.find(Poll.class, pollId);
        if (poll == null) {
            return null;
        }
        
        if (poll.getVisibility() == Poll.Visibility.PUBLIC ||
           (poll.getCreator() != null && poll.getCreator().getUserId().equals(userId)) ||
           (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(userId))) {
            return poll;
        }
        return null;
    }
    
    @Override
    @Transactional
    public Poll updatePoll(Optional<Integer> durationDays, Integer pollId, Integer userId, List<Integer> newInvites) {
        Poll toUpdate = em.find(Poll.class, pollId);

        if (toUpdate == null || !toUpdate.getCreator().getUserId().equals(userId)) {
            return null;
        }

        if (newInvites != null && !newInvites.isEmpty()) {
            List<Integer> allInvites = Stream.concat(toUpdate.getInvitedUsers().stream(), newInvites.stream())
                                             .distinct()
                                             .collect(Collectors.toList());
            toUpdate.setInvitedUsers(allInvites);
        }

        durationDays.ifPresent(days -> {
            Instant newDeadline = toUpdate.getValidUntil().plus(Duration.ofDays(days));
            toUpdate.setValidUntil(newDeadline);
        });

        return toUpdate;
    }

    @Override
    @Transactional
    public boolean deletePoll(Integer pollId, Integer userId) {
        Poll pollToDelete = em.find(Poll.class, pollId);
        if (pollToDelete == null || !pollToDelete.getCreator().getUserId().equals(userId)) {
            return false;
        }
        em.createQuery("DELETE FROM Vote v WHERE v.chosenOption.poll.id = :id")
          .setParameter("id", pollId)
          .executeUpdate();

        em.createQuery("DELETE FROM VoteOption vo WHERE vo.poll.id = :id")
          .setParameter("id", pollId)
          .executeUpdate();

        em.remove(pollToDelete);
        return true;
    }
    
    @Override
    @Transactional
    public boolean castVote(Integer pollId, Optional<Integer> userId, Integer presentationOrder) {
        Poll poll = em.find(Poll.class, pollId);
        if (poll == null || Instant.now().isAfter(poll.getValidUntil())) {
            return false;
        }

        VoteOptionId optionId = new VoteOptionId(pollId, presentationOrder);
        VoteOption chosenOption = em.find(VoteOption.class, optionId);
        if (chosenOption == null) {
            return false;
        }

        User voter = userId.map(id -> em.find(User.class, id)).orElse(null);
        if (userId.isPresent() && voter == null) {
            return false;
        }

        if (voter != null) {
            if (poll.getVisibility() == Poll.Visibility.PRIVATE && !poll.getInvitedUsers().contains(voter.getUserId())) {
                return false;
            }

            List<Vote> existingVotes = em.createQuery(
                    "SELECT v FROM Vote v WHERE v.voter = :voter AND v.chosenOption.poll = :poll", Vote.class)
                .setParameter("voter", voter)
                .setParameter("poll", poll)
                .getResultList();

            if (poll.getMaxVotesPerUser() == 1) {
                if (!existingVotes.isEmpty()) {
                    Vote existingVote = existingVotes.get(0);

                    if (existingVote.getChosenOption().equals(chosenOption)) {
                        return true;
                    }

                    em.remove(existingVotes.get(0));
                }
            } else {
                if (existingVotes.size() >= poll.getMaxVotesPerUser()) {
                    throw new IllegalStateException("Vote limit reached for this poll.");
                }
            }
        }

        Vote newVote = new Vote();
        newVote.setVoter(voter);
        newVote.setPublishedAt(Instant.now());
        newVote.setChosenOption(chosenOption);
        em.persist(newVote);

        System.out.println("Vote saved to DB. Publishing Kafka event for pollId: " + pollId);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("pollId", pollId);

        String topicName = pollTopicManager.getTopicNameForPoll(pollId);

        producerService.sendEvent(topicName, eventData);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getPollResults(Integer pollId) {
        List<Object[]> results = em.createQuery(
            "SELECT vo.caption, COUNT(v.id) FROM Vote v JOIN v.chosenOption vo WHERE vo.poll.id = :pollId GROUP BY vo.caption", Object[].class)
            .setParameter("pollId", pollId)
            .getResultList();
        
        Map<String, Integer> resultMap = new HashMap<>();
        for (Object[] result : results) {
            resultMap.put((String) result[0], ((Long) result[1]).intValue());
        }
        return resultMap;
    }

    @Override
    public void loginUser(Integer userId) {}

    @Override
    public void logoutUser(Integer userId) {}

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
    @Transactional
    public Poll addOptionsToPoll(Integer pollId, Integer userId, List<VoteOption> newOptions) {
        Poll poll = em.find(Poll.class, pollId);
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
            em.persist(vo);          
            poll.getPollOptions().add(vo);
        }
        em.flush();
        return poll;
}
}
