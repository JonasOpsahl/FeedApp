package com.gruppe2.backend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.gruppe2.backend.model.Poll;
import com.gruppe2.backend.model.User;
import com.gruppe2.backend.model.VoteOption;
import com.gruppe2.backend.model.Comment;


import redis.clients.jedis.UnifiedJedis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
public class CachingPollService implements PollService, CommentService {

    private Integer CACHE_TTL_SECONDS = 3600;
    private String LOGGED_IN_USERS_KEY = "users:loggedIn";
    private PollService delegate;
    private UnifiedJedis jedis;  

    private final CommentService commentsDelegate;


    public CachingPollService(@Qualifier("hibernatePollService") PollService delegate, JedisConnectionManager connectionManager) {
        this.delegate = delegate;
        this.jedis = connectionManager.getJedis();
        this.commentsDelegate = (CommentService) delegate; 

    }

    private String getPollCacheKey(Integer pollId) {
        return "poll:results:" + pollId;
    }

    @Override
    public Map<String, Integer> getPollResults(Integer pollId) {
        String cacheKey = getPollCacheKey(pollId);

        if (jedis.exists(cacheKey)) {
            System.out.println("Found cached results for poll " + pollId);
            Map<String, String> cachedResults = jedis.hgetAll(cacheKey);
            return cachedResults.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.parseInt(e.getValue())));
        }

        System.out.println("No cache results for poll " + pollId + ". Fetching from database.");
        Map<String, Integer> dbResults = delegate.getPollResults(pollId);

        if (dbResults != null && !dbResults.isEmpty()) {
            Map<String, String> resultsToCache = dbResults.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

            jedis.hset(cacheKey, resultsToCache);
            jedis.expire(cacheKey, CACHE_TTL_SECONDS);
        }
        return dbResults;
    }

    @Override
    public boolean castVote(Integer pollId, Optional<Integer> userId, Integer presentationOrder) {
        return delegate.castVote(pollId, userId, presentationOrder);
    }

    @Override
    public boolean deletePoll(Integer pollId, Integer userId) {
        boolean success = delegate.deletePoll(pollId, userId);
        if (success) {
            try {
                System.out.println("Poll " + pollId + " deleted, invalidating cache");
                jedis.del(getPollCacheKey(pollId));
            } catch (Exception e) {
                System.out.println("Cache delete failed: " + e.getMessage());
            }
        }
        return success;
    }

    @Override
    public void invalidatePollCache(Integer pollId) {
        System.out.println("Vote change detected for poll " + pollId + ", invalidating cache.");
        jedis.del(getPollCacheKey(pollId));
    }

  
    @Override public User createUser(String u, String e, String p) { return delegate.createUser(u, e, p); }
    @Override public List<User> getUsers() { return delegate.getUsers(); }
    @Override public User getUser(Integer id) { return delegate.getUser(id); }
    @Override public User updateUser(Integer id, Optional<String> u, Optional<String> e, Optional<String> p) { return delegate.updateUser(id, u, e, p); }
    @Override public Poll createPoll(String q, Integer d, Integer c, Poll.Visibility v, Optional<Integer> m, List<Integer> i, List<VoteOption> o) { return delegate.createPoll(q, d, c, v, m, i, o); }
    @Override public List<Poll> getPolls(Optional<Integer> id) { return delegate.getPolls(id); }
    @Override public Poll getPoll(Integer pId, Integer uId) { return delegate.getPoll(pId, uId); }
    @Override public Poll updatePoll(Optional<Integer> d, Integer pId, Integer uId, List<Integer> i) { return delegate.updatePoll(d, pId, uId, i); }


    @Override
    public boolean deleteUser(Integer id) {
        boolean success = delegate.deleteUser(id);
        if (success) {
            logoutUser(id);
        }
        return success;
    }


    @Override
    public void loginUser(Integer userId) {
        System.out.println("Cache: Logging in user "+userId);
        jedis.sadd(LOGGED_IN_USERS_KEY, userId.toString());
    }

    @Override
    public void logoutUser(Integer userId) {
        System.out.println("Cache: Logging out user "+userId);
        jedis.srem(LOGGED_IN_USERS_KEY, userId.toString());
    }

    @Override
    public boolean isUserLoggedIn(Integer userId) {
        return jedis.sismember(LOGGED_IN_USERS_KEY, userId.toString());
    }

    @Override
    public Set<String> getLoggedInUsers() {
        return jedis.smembers(LOGGED_IN_USERS_KEY);
    }

    @Override
    public Poll addOptionsToPoll(Integer pollId, Integer userId, List<VoteOption> newOptions) {
        Poll p = delegate.addOptionsToPoll(pollId, userId, newOptions);
        if (p != null) invalidatePollCache(pollId);
        return p;
    }

    @Override public Comment addComment(Integer pollId, Integer authorId, String content, Optional<Integer> parentId) {
        return commentsDelegate.addComment(pollId, authorId, content, parentId);
    }
    @Override public List<Comment> getTopLevel(Integer pollId, int offset, int limit) {
        return commentsDelegate.getTopLevel(pollId, offset, limit);
    }
    @Override public long countTopLevel(Integer pollId) { return commentsDelegate.countTopLevel(pollId); }
    @Override public List<Comment> getReplies(Integer pollId, Integer parentId, int offset, int limit) {
        return commentsDelegate.getReplies(pollId, parentId, offset, limit);
    }
    @Override public long countReplies(Integer pollId, Integer parentId) {
        return commentsDelegate.countReplies(pollId, parentId);
    }

    @Override public Optional<Comment> findById(Integer commentId) { return commentsDelegate.findById(commentId); }
    @Override public Comment updateContent(Integer commentId, String newContent, Integer requesterId) { return commentsDelegate.updateContent(commentId, newContent, requesterId); }
    @Override public void delete(Integer commentId, Integer requesterId) { commentsDelegate.delete(commentId, requesterId); }

}
