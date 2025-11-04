import { useState, useEffect } from "react";
import styles from "./App.module.css";
import CreatePoll from "./components/CreatePoll";
import VoteOnPoll from "./components/VoteOnPoll";
import Login from "./components/Login";
import { useAuth } from "./Auth";
import { fetchVisiblePolls, type Poll } from "./api";
import WebSocketMessages from "./components/WebSocketMessages";
import { connectWs, onWs, type WsEvent } from "./ws";


function App() {
  const { currentUser, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<"create" | "vote">("vote");
  const [polls, setPolls] = useState<Poll[]>([]);
  const [isLoginView, setIsLoginView] = useState(false);
  const [wsMessages, setWsMessages] = useState<string[]>([]);
  const [commentVersionByPoll, setCommentVersionByPoll] = useState<Record<number, number>>({}); 


  const refreshPolls = () => {
    fetchVisiblePolls(currentUser?.id)
      .then(setPolls)
      .catch((err) => console.error("Failed to load polls:", err));
  };

  useEffect(() => {
    refreshPolls();
  }, [currentUser?.id]);

  useEffect(() => {
    connectWs();
    const off = onWs((msg) => {
      if (typeof msg === "string") {
        if (msg === "pollsUpdated") refreshPolls();
        setWsMessages((prev) => [msg, ...prev].slice(0, 25));
        return;
      }

      const e = msg as WsEvent;

      if (e.type === "poll-created" || e.type === "poll-deleted" || e.type === "poll-updated") {
        refreshPolls();
      } else if (e.type === "vote-delta") {
        setWsMessages((p) => [
          `vote-delta poll=${(e as any).pollId} optionOrder=${(e as any).optionOrder}`,
          ...p,
        ].slice(0, 25));
      } else if (
        (e as any).type === "comment-created" ||
        (e as any).type === "comment-updated" ||
        (e as any).type === "comment-deleted"
      ) {
        const pid = (e as any).pollId;
        if (typeof pid === "number") {
          setCommentVersionByPoll((prev) => ({
            ...prev,
            [pid]: (prev[pid] ?? 0) + 1,
          }));
        }
      }
    });
    return () => {
      off();
    };
  }, []);

  const handlePollCreated = () => {
    refreshPolls();
    setActiveTab("vote");
  };

  if (isLoginView) {
    return <Login onLoginSuccess={() => setIsLoginView(false)} />;
  }

  return (
    <div className={styles.appContainer}>
      <WebSocketMessages messages={wsMessages} />
      <header className={styles.header}>
        {currentUser ? (
          <>
            <span>
              Welcome, <strong>{currentUser.username}</strong>! (userID: {currentUser.id})
            </span>
            <div style={{ display: "flex", gap: 8 }}>
              {/* REMOVE the old global Edit button here */}
              <button onClick={logout} className={styles.logoutButton}>Logout</button>
            </div>
          </>
        ) : (
          <>
            <span>Welcome, Guest!</span>
            <button onClick={() => setIsLoginView(true)} className={styles.submitButton}>
              Login / Register
            </button>
          </>
        )}
      </header>

      <nav className={styles.tabContainer}>
        {currentUser && (
          <button
            className={`${styles.tabButton} ${activeTab === "create" ? styles.tabButtonActive : ""}`}
            onClick={() => setActiveTab("create")}
          >
            Create Poll
          </button>
        )}
        <button
          className={`${styles.tabButton} ${activeTab === "vote" ? styles.tabButtonActive : ""}`}
          onClick={() => setActiveTab("vote")}
        >
          Vote on Polls
        </button>
      </nav>

      <main className={styles.tabContent}>
        {activeTab === "create" && currentUser && (
          <CreatePoll onPollCreated={handlePollCreated} />
        )}

        {activeTab === "vote" && (
          <div>
            <h2>Available Polls</h2>
            {polls.length > 0 ? (
              <div className={styles.pollListContainer}>
                {polls.map((poll) => (
                  <VoteOnPoll
                    key={`${poll.pollId}-${commentVersionByPoll[poll.pollId] ?? 0}`} 
                    pollData={poll}
                    onChanged={refreshPolls}
                  />
                ))}
              </div>
            ) : (
              <p>No polls to vote on right now</p>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;