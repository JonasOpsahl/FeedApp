package com.exp2.api.service;

import com.exp2.api.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("hibernatePollService")
@Profile("database")
public class HibernatePollService implements PollService {

    private final EntityManagerFactory emf;
    private final PollTopicManager pollTopicManager;

    public HibernatePollService(EntityManagerFactory emf, PollTopicManager pollTopicManager) {
        this.emf = emf;
        this.pollTopicManager = pollTopicManager;
    }

    // Users

    @Override
    public User createUser(String username, String email, String password) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(password);
            em.persist(newUser);
            em.getTransaction().commit();
            return newUser;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not create user", e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<User> getUsers() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u", User.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public User getUser(Integer userId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(User.class, userId);
        } finally {
            em.close();
        }
    }

    @Override
    public User updateUser(Integer userId, Optional<String> newUsername, Optional<String> newEmail, Optional<String> newPassword) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User toChange = em.find(User.class, userId);
            if (toChange != null) {
                newUsername.ifPresent(toChange::setUsername);
                newEmail.ifPresent(toChange::setEmail);
                newPassword.ifPresent(toChange::setPassword);
            }
            em.getTransaction().commit();
            return toChange;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not update user", e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deleteUser(Integer id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User toDelete = em.find(User.class, id);
            if (toDelete != null) {
                em.remove(toDelete);
            }
            em.getTransaction().commit();
            return toDelete != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not delete user", e);
        } finally {
            em.close();
        }
    }

    // Polls

    @Override
    public Poll createPoll(String question, Integer durationDays, Integer creatorId,
                        Poll.Visibility visibility,
                        Optional<Integer> maxVotesPerUser,
                        List<Integer> invitedUsers, List<VoteOption> pollOptions) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            
            User creator = em.find(User.class, creatorId);
            if (creator == null) {
                throw new IllegalArgumentException("User with ID " + creatorId + " doesnt exist.");
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
            em.getTransaction().commit();

            if (newPoll.getPollId() != null) {
                pollTopicManager.createPollTopic(newPoll.getPollId());
            }

            return newPoll;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not create poll", e);
        } finally {
            em.close();
        }
    }


    @Override
    public List<Poll> getPolls(Optional<Integer> userId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<Poll> allPolls = em.createQuery("SELECT p FROM Poll p", Poll.class).getResultList();

            List<Poll> visiblePolls;
            if (userId.isEmpty()) {
                visiblePolls = allPolls.stream()
                    .filter(poll -> poll.getVisibility() == Poll.Visibility.PUBLIC)
                    .collect(Collectors.toList());
            } else {
                Integer id = userId.get();
                visiblePolls = allPolls.stream()
                    .filter(poll -> 
                        poll.getVisibility() == Poll.Visibility.PUBLIC ||
                        poll.getCreator().getUserId().equals(id) ||
                        (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(id))
                    )
                    .collect(Collectors.toList());
            }

            for (Poll poll : visiblePolls) {
                poll.getPollOptions().size();
            }

            return visiblePolls;

        } finally {
            em.close();
        }
    }

    @Override
    public Poll getPoll(Integer pollId, Integer userId) {
        EntityManager em = emf.createEntityManager();
        try {
            Poll poll = em.find(Poll.class, pollId);
            if (poll == null) {
                return null;
            }
            if (poll.getVisibility() == Poll.Visibility.PUBLIC || 
               (poll.getCreator().getUserId().equals(userId)) || 
               (poll.getVisibility() == Poll.Visibility.PRIVATE && poll.getInvitedUsers().contains(userId))) {
                return poll;
            }
            return null;
        } finally {
            em.close();
        }
    }
    
    @Override
    public Poll updatePoll(Optional<Integer> durationDays, Integer pollId, Integer userId, List<Integer> newInvites) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Poll toUpdate = em.find(Poll.class, pollId);

            if (toUpdate == null || !toUpdate.getCreator().getUserId().equals(userId)) {
                em.getTransaction().rollback();
                return null; 
            }

            if (newInvites != null && !newInvites.isEmpty()) {
                List<Integer> currentInvites = toUpdate.getInvitedUsers();
                List<Integer> allInvites = Stream.concat(currentInvites.stream(), newInvites.stream())
                                                 .distinct()
                                                 .collect(Collectors.toList());
                toUpdate.setInvitedUsers(allInvites);
            }

            durationDays.ifPresent(days -> {
                Instant newDeadline = toUpdate.getValidUntil().plus(Duration.ofDays(days));
                toUpdate.setValidUntil(newDeadline);
            });

            em.getTransaction().commit();
            return toUpdate;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not update poll", e);
        } finally {
            em.close();
        }
    }


    @Override
    public boolean deletePoll(Integer pollId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Poll pollToDelete = em.find(Poll.class, pollId);
            if (pollToDelete != null) {
                em.remove(pollToDelete);
            }
            em.getTransaction().commit();
            return pollToDelete != null;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not delete poll", e);
        } finally {
            em.close();
        }
    }
    
    @Override
    public boolean castVote(Integer pollId, Optional<Integer> userId, Integer presentationOrder) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Poll poll = em.find(Poll.class, pollId);
            if (poll == null || Instant.now().isAfter(poll.getValidUntil())) {
                em.getTransaction().rollback();
                return false; 
            }

            VoteOptionId optionId = new VoteOptionId(pollId, presentationOrder);
            VoteOption chosenOption = em.find(VoteOption.class, optionId);
            if (chosenOption == null) {
                em.getTransaction().rollback();
                return false;
            }

            User voter = userId.map(id -> em.find(User.class, id)).orElse(null);
            if (userId.isPresent() && voter == null) {
                em.getTransaction().rollback();
                return false;
            }

            if (voter != null) {
                 if (poll.getVisibility() == Poll.Visibility.PRIVATE && !poll.getInvitedUsers().contains(voter.getUserId())) {
                    em.getTransaction().rollback();
                    return false;
                }
                long userVoteCount = em.createQuery("SELECT COUNT(v) FROM Vote v WHERE v.voter = :voter AND v.chosenOption.poll = :poll", Long.class)
                                       .setParameter("voter", voter)
                                       .setParameter("poll", poll)
                                       .getSingleResult();
                if (userVoteCount >= poll.getMaxVotesPerUser()) {
                    em.getTransaction().rollback();
                    return false; 
                }
            }

            Vote newVote = new Vote();
            newVote.setVoter(voter);
            newVote.setPublishedAt(Instant.now());
            newVote.setChosenOption(chosenOption);
            em.persist(newVote);

            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Could not cast vote", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Map<String, Integer> getPollResults(Integer pollId) {
        EntityManager em = emf.createEntityManager();
        try {
            List<Object[]> results = em.createQuery(
                "SELECT vo.caption, COUNT(v.id) FROM Vote v JOIN v.chosenOption vo WHERE vo.poll.id = :pollId GROUP BY vo.caption", Object[].class)
                .setParameter("pollId", pollId)
                .getResultList();
            
            Map<String, Integer> resultMap = new HashMap<>();
            for (Object[] result : results) {
                resultMap.put((String) result[0], ((Long) result[1]).intValue());
            }
            return resultMap;
        } finally {
            em.close();
        }
    }

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