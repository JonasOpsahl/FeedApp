package com.gruppe2.FeedApp.Services;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import com.gruppe2.FeedApp.Domain.*;

@Service
@Transactional
public class PollManager {
    private final EntityManager em;

    @Autowired
    public PollManager(EntityManager em) {
        this.em = em;
    }

    public int getNetVotesForOption(Long pollId, Long optionId) {
        Poll poll = em.find(Poll.class, pollId);
        if (poll == null) return 0;
        int net = 0;
        for (VoteOption option : poll.getOptions()) {
            if (option.getId().equals(optionId)) {
                String jpql = "SELECT v FROM Vote v WHERE v.poll.id = :pollId AND v.option.id = :optionId";
                List<Vote> votes = em.createQuery(jpql, Vote.class)
                        .setParameter("pollId", pollId)
                        .setParameter("optionId", optionId)
                        .getResultList();
                for (Vote v : votes) {
                    net += v.isUpvote() ? 1 : -1;
                }
            }
        }
        return net;
    }

    public User registerUser(String username, String password, String email) {
        String jpql = "SELECT u FROM User u WHERE LOWER(u.username) = :username";
        List<User> existing = em.createQuery(jpql, User.class)
                .setParameter("username", username.toLowerCase())
                .getResultList();
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Username already exists");
        }
    User u = new User();
    u.setUsername(username);
    u.setPassword(password);
    u.setEmail(email);
    em.persist(u);
    return u;
    }

    public Optional<User> loginUser(String username, String password) {
        String jpql = "SELECT u FROM User u WHERE LOWER(u.username) = :username AND u.password = :password";
        List<User> users = em.createQuery(jpql, User.class)
                .setParameter("username", username.toLowerCase())
                .setParameter("password", password)
                .getResultList();
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public Optional<User> findUser(Long id) {
        User u = em.find(User.class, id);
        return Optional.ofNullable(u);
    }
    public List<User> allUsers() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public Poll createPoll(Long creatorUserId,
                           String question,
                           boolean isPublic,
                           Instant publishedAt,
                           Instant validUntil,
                           Integer maxVotesPerUser,
                           Collection<String> invitedUsernames,
                           Collection<VoteOption> options) {
        User creator = em.find(User.class, creatorUserId);
        if (creator == null) throw new IllegalArgumentException("User not found");
    Poll p = new Poll();
        p.setCreatedBy(creator);
        p.setQuestion(question);
        p.setPublicPoll(isPublic);
        p.setPublishedAt(publishedAt);
        p.setValidUntil(validUntil);
        p.setMaxVotesPerUser(maxVotesPerUser);
        if (invitedUsernames != null) p.getInvitedUsernames().addAll(invitedUsernames);
        if (options != null) {
            for (VoteOption vo : options) {
                vo.setPoll(p);
                p.getOptions().add(vo);
                em.persist(vo);
            }
            p.getOptions().sort(Comparator.comparingInt(VoteOption::getPresentationOrder));
        }
        em.persist(p);
        return p;
    }

    public Optional<Poll> findPoll(Long id) {
        Poll p = em.find(Poll.class, id);
        return Optional.ofNullable(p);
    }
    public List<Poll> allPolls() {
        return em.createQuery("SELECT p FROM Poll p", Poll.class).getResultList();
    }

    
    public Vote castVote(Long pollId, Long optionId, Long voterUserId, boolean anonymous) {
        return castVote(pollId, optionId, voterUserId, anonymous, true);
    }

    @CacheEvict(value = "poll-vote-counts", key = "#pollId")
    public Vote castVote(Long pollId, Long optionId, Long voterUserId, boolean anonymous, boolean isUpvote) {
        Poll poll = em.find(Poll.class, pollId);
        if (poll == null) throw new NoSuchElementException("Poll not found: " + pollId);
        VoteOption option = em.find(VoteOption.class, optionId);
        if (option == null) throw new NoSuchElementException("Option not found: " + optionId);
        Vote v = new Vote();
        v.setPoll(poll);
        v.setOption(option);
        v.setVoterUserId(voterUserId);
        v.setAnonymous(anonymous);
        v.setPublishedAt(Instant.now());
        v.setUpvote(isUpvote);
        em.persist(v);
        
        return v;
    }

    
    public Optional<Vote> findVote(Long id) {
        Vote v = em.find(Vote.class, id);
        return Optional.ofNullable(v);
    }

    public List<Poll> publicPolls() {
        return em.createQuery("SELECT p FROM Poll p WHERE p.publicPoll = true", Poll.class).getResultList();
    }

    public List<Poll> privatePollsVisibleTo(String username) {
    String jpql = "SELECT DISTINCT p FROM Poll p LEFT JOIN p.invitedUsernames u " +
        "WHERE p.publicPoll = false AND (u = :username OR LOWER(p.createdBy.username) = LOWER(:username))";
    return em.createQuery(jpql, Poll.class)
        .setParameter("username", username)
        .getResultList();
    }

    public void deletePoll(Long pollId) {
        Poll p = em.find(Poll.class, pollId);
        if (p == null) return;
        String jpql = "SELECT v FROM Vote v WHERE v.poll.id = :pollId";
        List<Vote> votes = em.createQuery(jpql, Vote.class)
                .setParameter("pollId", pollId)
                .getResultList();
        for (Vote v : votes) {
            em.remove(v);
        }
        em.remove(p);
    }

    @CacheEvict(value = "poll-vote-counts", key = "#pollId")
    public Vote castOrChangeVote(Long pollId, Long optionId, Long voterUserId, boolean anonymous, boolean isUpvote) {
        Poll poll = em.find(Poll.class, pollId);
        if (poll == null) throw new NoSuchElementException("Poll not found: " + pollId);
        VoteOption option = em.find(VoteOption.class, optionId);
        if (option == null) throw new NoSuchElementException("Option not found: " + optionId);
        Instant now = Instant.now();
        if (poll.getPublishedAt() != null && now.isBefore(poll.getPublishedAt())) {
            throw new IllegalStateException("Poll not yet published");
        }
        if (poll.getValidUntil() != null && now.isAfter(poll.getValidUntil())) {
            throw new IllegalStateException("Poll deadline has passed");
        }
        if (!poll.isPublicPoll() && poll.getMaxVotesPerUser() != null && voterUserId != null) {
            String jpql = "SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId AND v.voterUserId = :voterUserId";
            Long userVotes = em.createQuery(jpql, Long.class)
                    .setParameter("pollId", pollId)
                    .setParameter("voterUserId", voterUserId)
                    .getSingleResult();
            if (userVotes >= poll.getMaxVotesPerUser()) {
                throw new IllegalStateException("Max votes per user reached");
            }
        }
    List<Vote> existingVotes;
    if (voterUserId == null) {
        String jpqlAnon = "SELECT v FROM Vote v WHERE v.poll.id = :pollId AND v.option.id = :optionId AND v.voterUserId IS NULL";
        existingVotes = em.createQuery(jpqlAnon, Vote.class)
            .setParameter("pollId", pollId)
            .setParameter("optionId", optionId)
            .getResultList();
    } else {
        String jpql = "SELECT v FROM Vote v WHERE v.poll.id = :pollId AND v.option.id = :optionId AND v.voterUserId = :voterUserId";
        existingVotes = em.createQuery(jpql, Vote.class)
            .setParameter("pollId", pollId)
            .setParameter("optionId", optionId)
            .setParameter("voterUserId", voterUserId)
            .getResultList();
    }
        Vote updatedVote = null;
        if (!existingVotes.isEmpty()) {
            updatedVote = existingVotes.get(0);
            updatedVote.setUpvote(isUpvote);
            updatedVote.setAnonymous(anonymous);
            updatedVote.setPublishedAt(now);
            em.merge(updatedVote);
        } else {
            updatedVote = castVote(pollId, optionId, voterUserId, anonymous, isUpvote);
        }
        
        return updatedVote;
    }

    public List<Vote> votesForPollLatestPerUser(Long pollId) {
        String jpql = "SELECT v FROM Vote v WHERE v.poll.id = :pollId ORDER BY v.publishedAt DESC";
        List<Vote> allVotes = em.createQuery(jpql, Vote.class)
                .setParameter("pollId", pollId)
                .getResultList();
        Map<Long, Vote> latest = new HashMap<>();
        for (Vote v : allVotes) {
            Long key = v.getVoterUserId();
            if (key != null && !latest.containsKey(key)) {
                latest.put(key, v);
            }
        }
        return new ArrayList<>(latest.values());
    }

    public List<Vote> votesForPoll(Long pollId) {
        String jpql = "SELECT v FROM Vote v WHERE v.poll.id = :pollId";
        return em.createQuery(jpql, Vote.class)
                .setParameter("pollId", pollId)
                .getResultList();
    }

    
    @Cacheable(value = "poll-vote-counts", key = "#pollId")
    public Map<Long, Integer> getVoteCountsForPoll(Long pollId) {
        return computeVoteCountsFromDatabase(pollId);
    }

    
    private Map<Long, Integer> computeVoteCountsFromDatabase(Long pollId) {
        String jpql = """
            SELECT v.option.id, COUNT(v.id) 
            FROM Vote v 
            WHERE v.poll.id = :pollId 
            GROUP BY v.option.id
            """;
        
        List<Object[]> results = em.createQuery(jpql, Object[].class)
                .setParameter("pollId", pollId)
                .getResultList();
        
        Map<Long, Integer> voteCounts = new HashMap<>();
        for (Object[] result : results) {
            Long optionId = (Long) result[0];
            Long count = (Long) result[1];
            voteCounts.put(optionId, count.intValue());
        }
        
        return voteCounts;
    }

}