import {
  createContext,
  useState,
  useContext,
  type FC,
  type ReactNode,
} from "react";
import { fetchAllUsers } from "./api";

interface User {
  id: number;
  username: string;
}

interface AuthContextType {
  currentUser: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  const login = async (username: string, _password: string) => {
    console.log("Attempting login with:", username);

    const users = await fetchAllUsers();

    const foundUser = users.find((user) => user.username === username);

    if (foundUser) {
      setCurrentUser({ id: foundUser.userId, username: foundUser.username });
    } else {
      throw new Error("User not found");
    }
  };

  const logout = () => {
    setCurrentUser(null);
  };

  const value = { currentUser, login, logout };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
