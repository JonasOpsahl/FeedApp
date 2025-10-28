import { useState, type FC, type FormEvent, useEffect} from "react";
import styles from "../App.module.css";
import type { Poll as PollData } from "../api";
import { useAuth } from "../Auth";
import { castVote, getPollResults } from "../api";

import { connectWs, onWs } from "../ws";


interface VoteOnPollProps {
  pollData: PollData;
}

const VoteOnPoll: FC<VoteOnPollProps> = ({ pollData }) => {
  const { currentUser } = useAuth();
  const [selectedOptions, setSelectedOptions] = useState<string[]>([]);
  const [hasVoted, setHasVoted] = useState(false);
  const [results, setResults] = useState<Record<string, number>>({});


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

  // Subscribe to WebSocket for this poll and update results
  useEffect(() => {
    connectWs();
    const off = onWs((msg) => {
      try {
        // plain string messages (legacy)
        if (typeof msg === "string") {
          if (msg === "votesUpdated") {
            void loadResults();
            return;
          }
          if (msg.startsWith("vote-delta")) {
            const pollMatch = msg.match(/poll=(\d+)/);
            const orderMatch = msg.match(/optionOrder=(\d+)/) || msg.match(/optionOrder=(\d+)/i);
            const pollIdNum = pollMatch ? Number(pollMatch[1]) : undefined;
            const optionOrderNum = orderMatch ? Number(orderMatch[1]) : undefined;

            if (pollIdNum === pollData.pollId && optionOrderNum != null) {
              const opt = pollData.pollOptions.find((o) => o.presentationOrder === optionOrderNum);
              if (opt) {
                setResults((prev) => {
                  const next = { ...prev };
                  next[opt.caption] = (next[opt.caption] || 0) + 1;
                  return next;
                });
              } else {
                // unknown option -> reload data
                void loadResults();
              }
            }
            return;
          }
        }

        const payload = typeof msg === "string" ? JSON.parse(msg) : msg;
        if (!payload) return;

        if (payload.type === "vote-delta") {
          const pollIdNum = Number(payload.pollId ?? payload.poll ?? payload.poll_id);
          const optionOrderNum = Number(payload.optionOrder ?? payload.option_order ?? payload.option);

          if (pollIdNum === pollData.pollId && !Number.isNaN(optionOrderNum)) {
            const opt = pollData.pollOptions.find((o) => o.presentationOrder === optionOrderNum);
            if (opt) {
              setResults((prev) => {
                const next = { ...prev };
                next[opt.caption] = (next[opt.caption] || 0) + 1;
                return next;
              });
            } else {
              void loadResults();
            }
          }
        }
      } catch (e) {
        void loadResults();
      }
    });
    return () => {
      off();
    };
  }, [pollData.pollId]);

  // Listen for App-level vote-delta events
  useEffect(() => {
    const handler = (ev: Event) => {
      try {
        const ce = ev as CustomEvent<{ pollId: number; optionOrder: number }>;
        const { pollId, optionOrder } = ce.detail ?? {};
        if (pollId !== pollData.pollId) return;

        const opt = pollData.pollOptions.find((o) => o.presentationOrder === Number(optionOrder));
        if (!opt) {
          void loadResults();
          return;
        }

        setResults((prev) => {
          const next = { ...prev };
          next[opt.caption] = (next[opt.caption] || 0) + 1;
          return next;
        });
      } catch {
        void loadResults();
      }
    };

    window.addEventListener("vote-delta", handler as EventListener);
    return () => {
      window.removeEventListener("vote-delta", handler as EventListener);
    };
  }, [pollData.pollId, pollData.pollOptions]);


  const handleSelectionChange = (optionCaption: string) => {
    setSelectedOptions((prev) => {
      if (pollData.maxVotesPerUser > 1) {
        const isSelected = prev.includes(optionCaption);
        if (isSelected) {
          return prev.filter((item) => item !== optionCaption);
        } else if (prev.length < pollData.maxVotesPerUser) {
          return [...prev, optionCaption];
        }
      } else {
        return [optionCaption];
      }
      return prev;
    });
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (selectedOptions.length === 0) {
      alert("Please select at least one option to vote");
      return;
    }

    try {
      const votePromises = selectedOptions.map((optionCaption) => {
        const optionToVote = pollData.pollOptions.find(
          (opt) => opt.caption === optionCaption
        );

        if (!optionToVote) {
          throw new Error(`Option ${optionCaption} not found`);
        }

        return castVote(
          pollData.pollId,
          optionToVote.presentationOrder,
          currentUser?.id
        );
      });

      await Promise.all(votePromises);
      setHasVoted(true);

      // optimistic: increment locally so UI updates instantly (in case ws message is delayed)
      setResults((prev) => {
        const next = { ...prev };
        for (const caption of selectedOptions) {
          next[caption] = (next[caption] || 0) + 1;
        }
        return next;
      });

      // dispatch local vote-delta events so this tab updates the same way as others
      for (const caption of selectedOptions) {
        const opt = pollData.pollOptions.find((o) => o.caption === caption);
        if (opt) {
          try {
            window.dispatchEvent(new CustomEvent("vote-delta", {
              detail: { pollId: Number(pollData.pollId), optionOrder: Number(opt.presentationOrder) }
            }));
          } catch {
          }
        }
      }

      const updatedResults = await getPollResults(pollData.pollId);
      const normalized: Record<string, number> = {};
      for (const opt of pollData.pollOptions) normalized[opt.caption] = updatedResults[opt.caption] ?? 0;
      for (const k of Object.keys(updatedResults)) if (!(k in normalized)) normalized[k] = updatedResults[k];
      setResults(normalized);

    } catch (error) {
      console.error(error);
      alert("Failed to submit vote");
    }
  };

  const totalVotes = Object.values(results).reduce(
    (sum, count) => sum + count,
    0
  );

  return (
    <div className={styles.pollCard}>
      <h3>{pollData.question}</h3>
      <div className={styles.optionsList}>
        {pollData.pollOptions.map((option, index) => {
          const voteCount = results[option.caption] || 0;
          const percentage =
            totalVotes > 0 ? (voteCount / totalVotes) * 100 : 0;

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
                ></div>
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
          {pollData.pollOptions.map((option, index) => {
            return (
              <label
                key={index}
                onClick={() => handleSelectionChange(option.caption)}
                className={`${styles.voteOption} ${selectedOptions.includes(option.caption)
                  ? styles.voteOptionSelected
                  : ""
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
            );
          })}
          <button
            type="submit"
            className={styles.submitButton}
            style={{ marginTop: "20px" }}
          >
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
