package com.gruppe2.backend.service;

import com.gruppe2.backend.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentService {
    Comment addComment(Integer pollId, Integer authorId, String content, Optional<Integer> parentId);
    List<Comment> getTopLevel(Integer pollId, int offset, int limit);
    long countTopLevel(Integer pollId);
    List<Comment> getReplies(Integer pollId, Integer parentId, int offset, int limit);
    long countReplies(Integer pollId, Integer parentId);
    Optional<Comment> findById(Integer commentId);
    Comment updateContent(Integer commentId, String newContent);
    void delete(Integer commentId);
}