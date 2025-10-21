import { useState, type FC, type FormEvent } from "react";
import { useAuth } from "../Auth";
import styles from "../App.module.css";
import { createUser } from "../api";

interface LoginProps {
  onLoginSuccess: () => void;
}

const Login: FC<LoginProps> = ({ onLoginSuccess }) => {
  const [isRegistering, setIsRegistering] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const { login } = useAuth();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (isRegistering) {
      if (!email || !username || !password) {
        setError("All fields are required");
        return;
      }
      try {
        await createUser(username, email, password);
        setMessage("Account created successfully");
        setIsRegistering(false);
        setUsername("");
        setEmail("");
        setPassword("");
      } catch (err) {
        setError((err as Error).message || "Failed to create account");
      }
    } else {
      try {
        await login(username, password);
        onLoginSuccess();
      } catch (err) {
        setError("Login failed");
      }
    }
  };

  return (
    <div className={styles.appContainer}>
      <div className={styles.tabContent}>
        <h2>{isRegistering ? "Create an account" : "Login with username"}</h2>
        <form onSubmit={handleSubmit}>
          {message && <p style={{ color: "green" }}>{message}</p>}

          <div className={styles.formGroup}>
            <label className={styles.label}>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={styles.baseInput}
              required
            />
          </div>

          {isRegistering && (
            <div className={styles.formGroup}>
              <label className={styles.label}>Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={styles.baseInput}
                required
              />
            </div>
          )}

          <div className={styles.formGroup}>
            <label className={styles.label}>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.baseInput}
              required
            />
          </div>

          {error && <p style={{ color: "red" }}>{error}</p>}
          <button type="submit" className={styles.submitButton}>
            {isRegistering ? "Register" : "Login"}
          </button>
        </form>

        <p
          onClick={() => {
            setIsRegistering(!isRegistering);
            setError("");
            setMessage("");
          }}
          style={{
            cursor: "pointer",
            color: "var(--primary)",
            textAlign: "center",
            marginTop: "1rem",
          }}
        >
          {isRegistering
            ? "Login"
            : "Sign Up"}
        </p>
      </div>
    </div>
  );
};

export default Login;
