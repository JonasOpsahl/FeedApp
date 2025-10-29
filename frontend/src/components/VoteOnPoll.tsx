import { useState, type FC, type FormEvent, useEffect } from "react";
import styles from "../App.module.css";
import type { Poll as PollData } from "../api";
import { useAuth } from "../Auth";
import { castVote, getPollResults } from "../api";
import { onWs } from "../ws";
import confetti from "canvas-confetti"; 


interface VoteOnPollProps {
  pollData: PollData;
}

const VoteOnPoll: FC<VoteOnPollProps> = ({ pollData }) => {
  const { currentUser } = useAuth();
  const [selectedOptions, setSelectedOptions] = useState<string[]>([]);
  const [hasVoted, setHasVoted] = useState(false);
  const [results, setResults] = useState<Record<string, number>>({});

  // Confetti helper function
  const fireConfetti = () => {
    const duration = 900;
    const end = Date.now() + duration;
    const colors = ["#14b8a6", "#4ade80", "#60a5fa", "#facc15", "#f87171"];

    (function frame() {
      confetti({
        particleCount: 6,
        angle: 60,
        spread: 70,
        origin: { x: 0, y: 0.7 },
        colors,
        zIndex: 10000,
      });
      confetti({
        particleCount: 6,
        angle: 120,
        spread: 70,
        origin: { x: 1, y: 0.7 },
        colors,
        zIndex: 10000,
      });
      if (Date.now() < end) requestAnimationFrame(frame);
    })();
  };

  const loadResults = async () => {
    try {
      const pollResults = await getPollResults(pollData.pollId);

      const normalized: Record<string, number> = {};
      for (const opt of pollData.pollOptions) {
        normalized[opt.caption] = pollResults[opt.caption] ?? 0;
      }
      for (const k of Object.keys(pollResults)) {
        if (!(k in normalized)) normalized[k] = pollResults[k];
      }

      setResults(normalized);
    } catch (error) {
      console.error("Could not fetch poll results", error);
    }
  };

  useEffect(() => {
    void loadResults();
  }, [pollData.pollId]);

  // Subscribe to WebSocket messages and apply deltas immediately
   useEffect(() => {
    const off = onWs((msg) => {
      try {
        if (typeof msg === "string") {
          if (msg.startsWith("vote-delta")) {
            const pollMatch = msg.match(/poll=(\d+)/);
            const orderMatch = msg.match(/optionOrder=(\d+)/i);
            const pollIdNum = pollMatch ? Number(pollMatch[1]) : undefined;
            const optionOrderNum = orderMatch ? Number(orderMatch[1]) : undefined;

            if (pollIdNum === pollData.pollId && optionOrderNum != null) {
              const opt = pollData.pollOptions.find(
                (o) => o.presentationOrder === optionOrderNum
              );
              if (opt) {
                setResults((prev) => ({
                  ...prev,
                  [opt.caption]: (prev[opt.caption] || 0) + 1,
                }));
              } else {
                void loadResults();
              }
            }
          }
          return;
        }


       const payload = msg as any;
        if (payload?.type === "vote-delta") {
          const pollIdNum = Number(payload.pollId ?? payload.poll ?? payload.poll_id);
          const optionOrderNum = Number(payload.optionOrder ?? payload.option_order ?? payload.option);

          if (pollIdNum === pollData.pollId && !Number.isNaN(optionOrderNum)) {
            const opt = pollData.pollOptions.find(
              (o) => o.presentationOrder === optionOrderNum
            );
            if (opt) {
              setResults((prev) => ({
                ...prev,
                [opt.caption]: (prev[opt.caption] || 0) + 1,
              }));
            } else {
              void loadResults();
            }
          }
        }
      } catch {
        void loadResults();
      }
    });

    return () => { off(); };
  }, [pollData.pollId, pollData.pollOptions]);

  const handleSelectionChange = (optionCaption: string) => {
    setSelectedOptions((prev) => {
      if (pollData.maxVotesPerUser > 1) {
     
        if (prev.includes(optionCaption)) {
          return prev.filter((c) => c !== optionCaption);
        }
        if (prev.length < pollData.maxVotesPerUser) {
          return [...prev, optionCaption];
        }
        return prev; 
      } else {
      
        return [optionCaption];
      }
    });
  };


  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (selectedOptions.length === 0) {
      alert("Please select at least one option to vote");
      return;
    }

    try {
      // Post all votes; let WS delta update UI
      const votePromises = selectedOptions.map((caption) => {
        const optionToVote = pollData.pollOptions.find((opt) => opt.caption === caption);
        if (!optionToVote) throw new Error(`Option ${caption} not found`);
        return castVote(pollData.pollId, optionToVote.presentationOrder, currentUser?.id);
      });

      await Promise.all(votePromises);
      setHasVoted(true);
      fireConfetti(); 


   
      setTimeout(() => void loadResults(), 1000);
    } catch (error) {
      console.error(error);
      alert("Failed to submit vote");
    }
  };

  const totalVotes = Object.values(results).reduce((sum, count) => sum + count, 0);

  return (
    <div className={styles.pollCard}>
      <h3>{pollData.question}</h3>

      <div className={styles.optionsList}>
        {pollData.pollOptions.map((option, index) => {
          const voteCount = results[option.caption] || 0;
          const percentage = totalVotes > 0 ? (voteCount / totalVotes) * 100 : 0;

          return (
            <div key={index} className={styles.voteResultRow}>
              <div className={styles.voteOptionLabel}>
                <span>{option.caption}</span>
                <strong>
                  {voteCount} vote(s) ({percentage.toFixed(1)}%)
                </strong>
              </div>
              <div className={styles.progressBarContainer}>
                <div
                  className={styles.progressBar}
                  style={{ width: `${percentage}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {!hasVoted ? (
        <form onSubmit={handleSubmit} style={{ marginTop: "20px" }}>
          <p>
            <strong>Your Vote:</strong>
          </p>
          {pollData.pollOptions.map((option, index) => (
            <label
              key={index}
              onClick={() => handleSelectionChange(option.caption)}
              className={`${styles.voteOption} ${
                selectedOptions.includes(option.caption) ? styles.voteOptionSelected : ""
              }`}
            >
              <input
                type={pollData.maxVotesPerUser > 1 ? "checkbox" : "radio"}
                name={`pollOption-${pollData.pollId}`}
                value={option.caption}
                checked={selectedOptions.includes(option.caption)}
                onChange={() => handleSelectionChange(option.caption)}
                className={styles.hiddenInput}
              />
              {option.caption}
            </label>
          ))}
          <button type="submit" className={styles.submitButton} style={{ marginTop: "20px" }}>
            Submit Vote
          </button>
        </form>
      ) : (
        <p className={styles.votedMessage}>Vote submitted</p>
      )}
    </div>
  );
};

export default VoteOnPoll;