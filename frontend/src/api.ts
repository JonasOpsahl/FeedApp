// Helper to get the token from storage
const getAuthToken = (): string | null => {
  return localStorage.getItem("jwtToken");
};

/**
 * A wrapper for fetch that automatically adds the JWT Authorization header
 * and handles JSON request/response bodies.
 */
const apiFetch = async (
  endpoint: string,
  options: RequestInit = {}
): Promise<any> => {
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});

  // Add Authorization header if we have a token
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  // Set default Content-Type for JSON body if one is provided
  if (
    options.body &&
    !(options.body instanceof URLSearchParams) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(
      errorData.message ||
        `API Error: ${response.status} ${response.statusText}`
    );
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return;
  }

  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch {
    return text === "true";
  }
};

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

export const registerUser = async (
  username: string,
  email: string,
  password: string
): Promise<User> => {
  return apiFetch("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password }),
  });
};

// POLLS

export const fetchVisiblePolls = async (): Promise<Poll[]> => {
  const token = getAuthToken();
  const url = `${API_BASE_URL}/polls`;

  const headers: HeadersInit = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, { headers });
  if (!response.ok) throw new Error("Failed to fetch polls");
  return response.json();
};

export const fetchPollById = async (pollId: number): Promise<Poll> => {
  const token = getAuthToken();
  const url = `${API_BASE_URL}/polls/${pollId}`;

  const headers: HeadersInit = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, { headers });
  if (!response.ok) {
    throw new Error("Failed to fetch poll or you don't have permission");
  }
  return response.json();
};

export const createPoll = async (pollData: {
  question: string;
  durationDays: number;
  visibility: "PUBLIC" | "PRIVATE";
  maxVotesPerUser: number;
  invitedUsers: number[];
  optionCaptions: string[];
  optionOrders: number[];
}): Promise<Poll> => {
  const params = new URLSearchParams();
  params.append("question", pollData.question);
  params.append("durationDays", pollData.durationDays.toString());
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

  return apiFetch(`/polls`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString(),
  });
};

// VOTE

export const castVote = async (
  pollId: number,
  presentationOrder: number
): Promise<boolean> => {
  const params = new URLSearchParams();
  params.append("presentationOrder", presentationOrder.toString());

  return apiFetch(`/polls/${pollId}/vote`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString(),
  });
};

export const getPollResults = async (
  pollId: number
): Promise<Record<string, number>> => {
  const response = await fetch(`${API_BASE_URL}/polls/${pollId}/results`);
  if (!response.ok) throw new Error("Failed to fetch results");
  return response.json();
};

// POLL MANAGEMENT

export async function updatePollDurationDays(
  pollId: number,
  durationDays: number
) {
  const qs = new URLSearchParams({
    durationDays: String(Math.max(1, Math.ceil(durationDays))),
  });
  return apiFetch(`/polls/${pollId}?${qs.toString()}`, {
    method: "PUT",
  });
}

export async function deletePoll(pollId: number): Promise<void> {
  return apiFetch(`/polls/${pollId}`, {
    method: "DELETE",
  });
}

export async function addOptionsToPoll(
  pollId: number,
  captions: string[],
  orders: number[]
) {
  const form = new URLSearchParams();
  captions.forEach((c) => form.append("optionCaptions", c));
  orders.forEach((o) => form.append("optionOrders", String(o)));

  return apiFetch(`/polls/${pollId}/options`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form.toString(),
  });
}

// COMMENTS

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

export async function fetchComments(
  pollId: number,
  offset = 0,
  limit = 5
): Promise<CommentsPage<Comment>> {
  const res = await fetch(
    `/api/polls/${pollId}/comments?offset=${offset}&limit=${limit}`
  );
  if (!res.ok) throw new Error("Failed to fetch comments");
  return res.json();
}

export async function fetchReplies(
  pollId: number,
  parentId: number,
  offset = 0,
  limit = 3
): Promise<CommentsPage<Comment>> {
  const res = await fetch(
    `/api/polls/${pollId}/comments/replies?parentId=${parentId}&offset=${offset}&limit=${limit}`
  );
  if (!res.ok) throw new Error("Failed to fetch replies");
  return res.json();
}

export async function addComment(
  pollId: number,
  content: string,
  parentId?: number
): Promise<Comment> {
  return apiFetch(`/polls/${pollId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content, parentId }),
  });
}

export const updateComment = async (
  pollId: number,
  commentId: number,
  content: string
): Promise<Comment> => {
  return apiFetch(`/polls/${pollId}/comments/${commentId}`, {
    method: "PUT",
    body: JSON.stringify({ content }),
  });
};

export const deleteComment = async (
  pollId: number,
  commentId: number
): Promise<void> => {
  return apiFetch(`/polls/${pollId}/comments/${commentId}`, {
    method: "DELETE",
  });
};
