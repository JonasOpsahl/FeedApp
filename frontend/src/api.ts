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
): Promise<void> => {
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

  if (!response.ok) throw new Error("Failed to cast vote");
  return;
};

export const getPollResults = async (
  pollId: number
): Promise<Record<string, number>> => {
  const response = await fetch(`${API_BASE_URL}/polls/${pollId}/results`);
  if (!response.ok) throw new Error("Failed to fetch results");
  return response.json();
};
