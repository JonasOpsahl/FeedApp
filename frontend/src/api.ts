export interface User {
  userId: number;
  username: string;
  email: string;
}

export interface VoteOption {
  caption: string;
  presentationOrder: number;
}

export interface Poll {
  pollId: number;
  question: string;
  pollOptions: VoteOption[];
  creatorId: number;
  visibility: "PUBLIC" | "PRIVATE";
  maxVotesPerUser: number;
  invitedUsers: number[];
}

const API_BASE_URL = "/api";

// USER

export const fetchAllUsers = async (): Promise<User[]> => {
  const response = await fetch(`${API_BASE_URL}/users`);
  if (!response.ok) throw new Error("Failed to fetch users");
  return response.json();
};

export const createUser = async (
  username: string,
  email: string,
  password: string
): Promise<User> => {
  const params = new URLSearchParams();
  params.append("username", username);
  params.append("email", email);
  params.append("password", password);

  const response = await fetch(`${API_BASE_URL}/users`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString(),
  });

  if (!response.ok) {
    throw new Error(
      "Failed to create user"
    );
  }
  return response.json();
};

// POLLS

export const fetchVisiblePolls = async (userId?: number): Promise<Poll[]> => {
  const url = userId ? `${API_BASE_URL}/polls?userId=${userId}` : `${API_BASE_URL}/polls`;
  
  const response = await fetch(url);
  if (!response.ok) throw new Error("Failed to fetch polls");
  return response.json();
};

export const fetchPollById = async (
  pollId: number,
  userId: number
): Promise<Poll> => {
  const response = await fetch(
    `${API_BASE_URL}/polls/${pollId}?userId=${userId}`
  );
  if (!response.ok) {
    throw new Error("Failed to fetch poll or you don't have permission");
  }
  const poll = await response.json();
  if (!poll) {
    throw new Error("Failed to fetch poll or you don't have permission");
  }
  return poll;
};

export const createPoll = async (pollData: {
  question: string;
  durationDays: number;
  creatorId: number;
  visibility: "PUBLIC" | "PRIVATE";
  maxVotesPerUser: number;
  invitedUsers: number[];
  optionCaptions: string[];
  optionOrders: number[];
}): Promise<Poll> => {
  const params = new URLSearchParams();
  params.append("question", pollData.question);
  params.append("durationDays", pollData.durationDays.toString());
  params.append("creatorId", pollData.creatorId.toString());
  params.append("visibility", pollData.visibility);
  params.append("maxVotesPerUser", pollData.maxVotesPerUser.toString());

  pollData.invitedUsers.forEach((id) =>
    params.append("invitedUsers", id.toString())
  );
  pollData.optionCaptions.forEach((caption) =>
    params.append("optionCaptions", caption)
  );
  pollData.optionOrders.forEach((order) =>
    params.append("optionOrders", order.toString())
  );

  const response = await fetch(`${API_BASE_URL}/polls`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString(),
  });

  if (!response.ok) throw new Error("Failed to create poll");
  return response.json();
};

// VOTE

export const castVote = async (
  pollId: number,
  presentationOrder: number,
  userId?: number
): Promise<boolean> => {
  const params = new URLSearchParams();
  params.append("presentationOrder", presentationOrder.toString());
  if (userId) {
    params.append("userId", userId.toString());
  }

  const response = await fetch(`${API_BASE_URL}/polls/${pollId}/vote`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString(),
  });

  if (!response.ok) {
    console.error("Failed to cast vote:", response.status);
    return false;
  }

  const result = await response.json();
  return result === true;
};

export const getPollResults = async (
  pollId: number
): Promise<Record<string, number>> => {
  const response = await fetch(`${API_BASE_URL}/polls/${pollId}/results`);
  if (!response.ok) throw new Error("Failed to fetch results");
  return response.json();
};

export async function updatePollDurationDays(pollId: number, userId: number, durationDays: number) {
  const qs = new URLSearchParams({
    userId: String(userId),
    durationDays: String(Math.max(1, Math.ceil(durationDays))),
  });
  const res = await fetch(`/api/polls/${pollId}?${qs.toString()}`, {
    method: "PUT",
  });
  if (!res.ok) throw new Error(`Update deadline failed (${res.status})`);
  return res.json();
}

export async function deletePoll(pollId: number, userId: number): Promise<void> {
  const qs = new URLSearchParams({ userId: String(userId) });
  const res = await fetch(`/api/polls/${pollId}?${qs.toString()}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(`Delete failed (${res.status})`);
}

export async function addOptionsToPoll(pollId: number, userId: number, captions: string[], orders: number[]) {
  const form = new URLSearchParams();
  form.set("userId", String(userId));
  captions.forEach(c => form.append("optionCaptions", c));
  orders.forEach(o => form.append("optionOrders", String(o)));
  const res = await fetch(`/api/polls/${pollId}/options`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form.toString(),
  });
  if (!res.ok) throw new Error(`Add options failed (${res.status})`);
  return res.json();
}
// ...existing code...
export interface Comment {
  commentId: number;
  pollId?: number;
  parentId?: number | null;
  authorId: number;
  content: string;
  createdAt: string;
  updatedAt: string;
  parent?: Comment | null;
  
}

export interface CommentsPage<T> {
  items: T[];
  total: number;
  hasMore: boolean;
  nextOffset: number;
}

export async function fetchComments(pollId: number, offset = 0, limit = 5): Promise<CommentsPage<Comment>> {
  const res = await fetch(`/api/polls/${pollId}/comments?offset=${offset}&limit=${limit}`);
  if (!res.ok) throw new Error("Failed to fetch comments");
  return res.json();
}

export async function fetchReplies(pollId: number, parentId: number, offset = 0, limit = 3): Promise<CommentsPage<Comment>> {
  const res = await fetch(`/api/polls/${pollId}/comments/replies?parentId=${parentId}&offset=${offset}&limit=${limit}`);
  if (!res.ok) throw new Error("Failed to fetch replies");
  return res.json();
}

export async function addComment(pollId: number, authorId: number, content: string, parentId?: number): Promise<Comment> {
  const res = await fetch(`/api/polls/${pollId}/comments`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ authorId, content, parentId }),
  });
  if (!res.ok) throw new Error("Failed to add comment");
  return res.json();
}
export const updateComment = async (
  pollId: number,
  commentId: number,
  editorId: number,
  content: string
): Promise<Comment> => {
  const res = await fetch(`/api/polls/${pollId}/comments/${commentId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ editorId, content }),
  });
  if (!res.ok) throw new Error(`Failed to update comment`);
  return res.json();
};
export const deleteComment = async (
  pollId: number,
  commentId: number,
  requesterId: number
): Promise<void> => {
  const res = await fetch(
    `/api/polls/${pollId}/comments/${commentId}?requesterId=${requesterId}`,
    { method: "DELETE" }
  );
  if (!res.ok) throw new Error(`Failed to delete comment`);
};