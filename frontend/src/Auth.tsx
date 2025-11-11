import {
  createContext,
  useState,
  useContext,
  type FC,
  type ReactNode,
} from "react";

interface User {
  id: number;
  username: string;
}

// Store key
const TOKEN_KEY = "jwtToken";
const USER_KEY = "currentUser";

interface AuthContextType {
  currentUser: User | null;
  token: string | null; // Expose the token
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean; // Helper
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Helper function to get saved user
const getSavedUser = (): User | null => {
  try {
    const savedUser = localStorage.getItem(USER_KEY);
    return savedUser ? JSON.parse(savedUser) : null;
  } catch {
    return null;
  }
};

export const AuthProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(getSavedUser);
  const [token, setToken] = useState<string | null>(
    localStorage.getItem(TOKEN_KEY)
  );

  const login = async (username: string, password: string) => {
    console.log("Attempting login with:", username);

    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    const data = await res.json();

    if (res.ok) {
      const { token, userId, username: responseUsername } = data;
      const user: User = { id: userId, username: responseUsername };

      setToken(token);
      setCurrentUser(user);
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      throw new Error(data.message || "Invalid username or password");
    }
  };

  const logout = () => {
    setCurrentUser(null);
    setToken(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  };

  const value = {
    currentUser,
    token,
    login,
    logout,
    isAuthenticated: !!token, // True if token exists
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
