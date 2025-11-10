import { type FC, useEffect, useMemo, useState } from "react";
import styles from "../App.module.css";
import {
  addComment,
  fetchComments,
  fetchReplies,
  updateComment,
  deleteComment,
  type Comment,
  type CommentsPage,
  fetchAllUsers,
} from "../api";
import { useAuth } from "../Auth";
import { onWs } from "../ws";

interface CommentsSectionProps {
  pollId: number;
  ownerUserId?: number;
  initialTopLimit?: number;
  initialRepliesLimit?: number;
}

const CommentsSection: FC<CommentsSectionProps> = ({
  pollId,
  ownerUserId,
  initialTopLimit = 3,
  initialRepliesLimit = 2,
}) => {
  const { currentUser } = useAuth();
  const [top, setTop] = useState<CommentsPage<Comment> | null>(null);
  const [topOffset, setTopOffset] = useState(0);
  const [newContent, setNewContent] = useState("");
  const [usernames, setUsernames] = useState<Record<number, string>>({});

  // id -> replies page state
  type ReplyState = Record<
    number,
    { page: CommentsPage<Comment> | null; offset: number }
  >;
  const [repliesState, setRepliesState] = useState<ReplyState>({});

  // Username lookup
  useEffect(() => {
    fetchAllUsers()
      .then((users) => {
        console.log("Fetched users:", users);
        const entries = users.map((user) => {
          return [user.userId, user.username];
        });
        setUsernames(Object.fromEntries(entries));
      })
      .catch((err) => {
        console.error("Failed to fetch users for comments:", err);
      });
  }, []);

  // Load initial top-level comments
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const page = await fetchComments(pollId, 0, initialTopLimit);
        if (!cancelled) {
          setTop(page);
          setTopOffset(page.nextOffset);
        }
      } catch (e) {
        console.error(e);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [pollId, initialTopLimit]);

  // Prefetch replies to populate counts for visible top-level comments
  useEffect(() => {
    if (!top?.items?.length) return;
    // fetch first page of replies to obtain total count for each top-level comment
    top.items.forEach((c) => {
      ensureRepliesLoaded(c.commentId, true).catch(() => {});
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [top?.items]); // whenever top items change, refresh their replies metadata

  // Helpers for replies paging
  const ensureRepliesLoaded = async (parentId: number, reset = false) => {
    const rs = repliesState[parentId];
    const offset = reset || !rs ? 0 : rs.offset;
    const page = await fetchReplies(
      pollId,
      parentId,
      offset,
      initialRepliesLimit
    );
    setRepliesState((prev) => ({
      ...prev,
      [parentId]: { page, offset: page.nextOffset },
    }));
  };

  const loadMoreTop = async () => {
    const page = await fetchComments(pollId, topOffset, initialTopLimit);
    setTop((prev) =>
      prev
        ? {
            ...page,
            items: [...prev.items, ...page.items],
          }
        : page
    );
    setTopOffset(page.nextOffset);
  };

  const loadMoreReplies = async (parentId: number) => {
    const rs = repliesState[parentId];
    const offset = rs?.offset ?? 0;
    const page = await fetchReplies(
      pollId,
      parentId,
      offset,
      initialRepliesLimit
    );
    setRepliesState((prev) => ({
      ...prev,
      [parentId]: {
        page: rs?.page
          ? { ...page, items: [...rs.page.items, ...page.items] }
          : page,
        offset: page.nextOffset,
      },
    }));
  };

  // Create top-level comment
  const handleAdd = async () => {
    if (!currentUser) {
      alert("You must be logged in to comment.");
      return;
    }
    const content = newContent.trim();
    if (!content) return;
    const created = await addComment(pollId, content);
    setNewContent("");
    setTop((prev) =>
      prev
        ? { ...prev, items: [created, ...prev.items], total: prev.total + 1 }
        : { items: [created], total: 1, hasMore: false, nextOffset: 1 }
    );
  };

  // Create reply (works for any depth)
  const handleReply = async (parentId: number, content: string) => {
    if (!currentUser) {
      alert("You must be logged in to reply.");
      return;
    }
    const trimmed = content.trim();
    if (!trimmed) return;
    const created = await addComment(pollId, trimmed, parentId);
    setRepliesState((prev) => {
      const current = prev[parentId];
      const nextPage: CommentsPage<Comment> = current?.page
        ? {
            ...current.page,
            items: [...current.page.items, created],
            total: (current.page.total ?? 0) + 1,
            nextOffset: (current.page.nextOffset ?? 0) + 1,
            hasMore: false,
          }
        : { items: [created], total: 1, hasMore: false, nextOffset: 1 };
      return {
        ...prev,
        [parentId]: { page: nextPage, offset: nextPage.nextOffset },
      };
    });
  };

  // Local state updates for edit/delete
  const applyEditLocally = (commentId: number, content: string) => {
    setTop((prev) =>
      prev
        ? {
            ...prev,
            items: prev.items.map((i) =>
              i.commentId === commentId
                ? { ...i, content, updatedAt: new Date().toISOString() }
                : i
            ),
          }
        : prev
    );
    setRepliesState((prev) => {
      const next = { ...prev };
      for (const k of Object.keys(next)) {
        const rs = next[+k];
        if (rs?.page) {
          rs.page.items = rs.page.items.map((i) =>
            i.commentId === commentId
              ? { ...i, content, updatedAt: new Date().toISOString() }
              : i
          );
        }
      }
      return next;
    });
  };

  const applyDeleteLocally = (commentId: number) => {
    setTop((prev) => {
      if (!prev) return prev;
      const existed = prev.items.some((i) => i.commentId === commentId);
      return {
        ...prev,
        items: prev.items.filter((i) => i.commentId !== commentId),
        total: existed ? Math.max(0, prev.total - 1) : prev.total,
      };
    });
    setRepliesState((prev) => {
      const next = { ...prev };
      for (const k of Object.keys(next)) {
        const rs = next[+k];
        if (rs?.page) {
          const before = rs.page.items.length;
          rs.page.items = rs.page.items.filter(
            (i) => i.commentId !== commentId
          );
          if (rs.page.total && rs.page.items.length < before) {
            rs.page.total = Math.max(0, (rs.page.total ?? before) - 1);
          }
        }
      }
      return next;
    });
  };

  const handleEdit = async (commentId: number, content: string) => {
    if (!currentUser) return;
    await updateComment(pollId, commentId, content);
    applyEditLocally(commentId, content);
  };

  const handleDelete = async (commentId: number) => {
    if (!currentUser) return;
    await deleteComment(pollId, commentId);
    applyDeleteLocally(commentId);
  };

  // WS handling:
  useEffect(() => {
    const off = onWs(async (msg) => {
      if (typeof msg !== "object" || msg === null) return;
      const e = msg as any;

      // Normalize pollId
      const eventPollId =
        e.pollId ??
        e.id ??
        e.poll?.id ??
        e.poll?.pollId ??
        e.comment?.pollId ??
        e.comment?.poll?.pollId;

      if (eventPollId !== pollId) return;

      switch (e.type) {
        case "comment-created": // <- align with backend
        case "comment-added":
        case "reply-added": {
          const c = (e.comment as Comment) ?? e.payload ?? null;
          if (!c) return;

          // Normalize parent id (support many shapes)
          const parentId =
            e.parentId ??
            e.parentCommentId ??
            (c as any).parentId ??
            (c as any).parent_id ??
            (c as any).parent?.commentId ??
            null;

          if (!parentId) {
            // top-level
            setTop((prev) =>
              prev
                ? { ...prev, items: [c, ...prev.items], total: prev.total + 1 }
                : { items: [c], total: 1, hasMore: false, nextOffset: 1 }
            );
          } else {
            setRepliesState((prev) => {
              const current = prev[parentId];
              if (!current?.page) return prev;
              const nextPage = {
                ...current.page,
                items: [...current.page.items, c],
                total: (current.page.total ?? 0) + 1,
                hasMore: false,
                nextOffset: (current.page.nextOffset ?? 0) + 1,
              };
              return {
                ...prev,
                [parentId]: { page: nextPage, offset: nextPage.nextOffset },
              };
            });

            if (!repliesState[parentId]?.page) {
              await ensureRepliesLoaded(parentId, true);
            }
          }
          break;
        }
        case "comment-updated":
        case "comment-edited": {
          const id = e.commentId ?? e.comment?.commentId ?? e.id;
          const content = e.content ?? e.comment?.content;
          if (id && typeof content === "string") applyEditLocally(id, content);
          break;
        }
        case "comment-deleted": {
          const id = e.commentId ?? e.comment?.commentId ?? e.id;
          if (id) applyDeleteLocally(id);
          setRepliesState((prev) => {
            const copy = { ...prev };
            delete copy[id];
            return copy;
          });
          break;
        }
        default:
          break;
      }
    });
    return () => {
      off();
    };
  }, [pollId, repliesState]);

  const repliesStateOf = (parentId: number) =>
    repliesState[parentId]?.page || null;
  const onEnsureRepliesFor = (parentId: number) =>
    ensureRepliesLoaded(parentId);
  const onLoadMoreRepliesFor = (parentId: number) => loadMoreReplies(parentId);

  return (
    <div className={styles.commentsSection}>
      <h4 className={styles.commentsTitle}>Comments</h4>

      <div className={styles.commentList}>
        {top?.items.map((c) => (
          <CommentItem
            key={c.commentId}
            comment={c}
            repliesState={repliesState[c.commentId]?.page || null}
            onEnsureReplies={() => ensureRepliesLoaded(c.commentId)}
            onLoadMoreReplies={() => loadMoreReplies(c.commentId)}
            repliesStateOf={repliesStateOf}
            onEnsureRepliesFor={onEnsureRepliesFor}
            onLoadMoreRepliesFor={onLoadMoreRepliesFor}
            onReply={handleReply}
            userNameOf={(id) => usernames[id] ?? `user #${id}`}
            currentUserId={currentUser?.id ?? null}
            ownerUserId={ownerUserId}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />
        ))}
      </div>

      {top && top.hasMore && (
        <button className={styles.viewMoreBtn} onClick={loadMoreTop}>
          View more comments
        </button>
      )}

      <div style={{ marginTop: 8 }}>
        <textarea
          className={styles.commentTextarea}
          placeholder={currentUser ? "Write a comment..." : "Login to comment"}
          value={newContent}
          onChange={(e) => setNewContent(e.target.value)}
          disabled={!currentUser}
          rows={3}
        />
        <div className={styles.commentActions}>
          <button
            className={styles.commentSubmit}
            disabled={!currentUser || !newContent.trim()}
            onClick={handleAdd}
          >
            Post Comment
          </button>
        </div>
      </div>
    </div>
  );
};

const CommentItem: FC<{
  comment: Comment;
  repliesState: CommentsPage<Comment> | null;
  onEnsureReplies: () => Promise<void>;
  onLoadMoreReplies: () => Promise<void>;
  // for recursion
  repliesStateOf: (parentId: number) => CommentsPage<Comment> | null;
  onEnsureRepliesFor: (parentId: number) => Promise<void>;
  onLoadMoreRepliesFor: (parentId: number) => Promise<void>;

  onReply: (parentId: number, content: string) => void;
  userNameOf: (id: number) => string;
  currentUserId: number | null;
  ownerUserId?: number;
  onEdit: (commentId: number, content: string) => Promise<void>;
  onDelete: (commentId: number) => Promise<void>;
}> = ({
  comment,
  repliesState,
  onEnsureReplies,
  onLoadMoreReplies,
  repliesStateOf,
  onEnsureRepliesFor,
  onLoadMoreRepliesFor,
  onReply,
  userNameOf,
  currentUserId,
  ownerUserId,
  onEdit,
  onDelete,
}) => {
  const [showReplyBox, setShowReplyBox] = useState(false);
  const [replyContent, setReplyContent] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);

  useEffect(() => {
    if (!repliesState) onEnsureReplies().catch(() => {});
  }, [comment.commentId]);

  useEffect(() => {
    if (!repliesState?.items) return;
    for (const r of repliesState.items) {
      onEnsureRepliesFor(r.commentId).catch(() => {});
    }
  }, [repliesState?.items]);

  useEffect(() => {
    if (showReplyBox && !repliesState) onEnsureReplies();
  }, [showReplyBox]);

  const created = useMemo(
    () => new Date(comment.createdAt).toLocaleString(),
    [comment.createdAt]
  );

  const canEdit = currentUserId != null && comment.authorId === currentUserId;
  const canDelete =
    canEdit || (ownerUserId != null && currentUserId === ownerUserId);

  // Look at this later with group
  // Bug with show votes
  const totalDirect = repliesState?.total ?? 0;

  const countDescendants = (parentId: number): number => {
    const rs = repliesStateOf(parentId);
    if (!rs) return 0;
    const direct = rs.total ?? 0;
    if (!rs.items?.length) return direct;
    return rs.items.reduce(
      (sum, child) => sum + countDescendants(child.commentId),
      direct
    );
  };

  const totalNested = (repliesState?.items ?? []).reduce(
    (sum, child) => sum + countDescendants(child.commentId),
    0
  );

  const totalCount = totalDirect + totalNested;
  const hasReplies = totalCount > 0;

  const closedLabel = hasReplies ? `Show replies (${totalCount})` : "Reply";
  const openLabel = hasReplies ? `Hide replies (${totalCount})` : "Hide reply";

  return (
    <div className={styles.commentItem}>
      <div className={styles.commentMeta}>
        <span className={styles.commentAuthor}>
          {userNameOf(comment.authorId)}
        </span>
        <span title={comment.createdAt}>{created}</span>

        <div className={styles.commentActionsInline}>
          {canEdit && !isEditing && (
            <button
              className={styles.actionButton}
              onClick={() => setIsEditing(true)}
            >
              Edit
            </button>
          )}
          {canDelete && (
            <button
              className={`${styles.actionButton} ${styles.actionDanger}`}
              onClick={async () => {
                if (confirm("Delete this comment?"))
                  await onDelete(comment.commentId);
              }}
            >
              Delete
            </button>
          )}
        </div>
      </div>

      {!isEditing ? (
        <div className={styles.commentBody}>{comment.content}</div>
      ) : (
        <div className={styles.editArea}>
          <textarea
            className={styles.replyTextarea}
            rows={3}
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
          />
          <div className={styles.commentActions}>
            <button
              className={`${styles.actionButton} ${styles.actionPrimary}`}
              disabled={!editContent.trim()}
              onClick={async () => {
                await onEdit(comment.commentId, editContent.trim());
                setIsEditing(false);
              }}
            >
              Save
            </button>
            <button
              className={styles.actionButton}
              onClick={() => {
                setIsEditing(false);
                setEditContent(comment.content);
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <button
        className={styles.replyToggle}
        onClick={() => setShowReplyBox((s) => !s)}
      >
        {showReplyBox ? openLabel : closedLabel}
      </button>

      {showReplyBox && (
        <div className={styles.replies}>
          {repliesState?.items?.map((r) => (
            <div key={r.commentId} className={styles.replyItem}>
              <CommentItem
                comment={r}
                repliesState={repliesStateOf(r.commentId)}
                onEnsureReplies={() => onEnsureRepliesFor(r.commentId)}
                onLoadMoreReplies={() => onLoadMoreRepliesFor(r.commentId)}
                repliesStateOf={repliesStateOf}
                onEnsureRepliesFor={onEnsureRepliesFor}
                onLoadMoreRepliesFor={onLoadMoreRepliesFor}
                onReply={onReply}
                userNameOf={userNameOf}
                currentUserId={currentUserId}
                ownerUserId={ownerUserId}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            </div>
          ))}

          {repliesState && repliesState.hasMore && (
            <button className={styles.viewMoreBtn} onClick={onLoadMoreReplies}>
              View more replies
            </button>
          )}

          <div>
            <textarea
              className={styles.replyTextarea}
              placeholder="Write a reply..."
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              rows={2}
            />
            <div className={styles.commentActions}>
              <button
                className={styles.replySubmit}
                disabled={!replyContent.trim()}
                onClick={() => {
                  onReply(comment.commentId, replyContent.trim());
                  setReplyContent("");
                }}
              >
                Reply
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CommentsSection;
