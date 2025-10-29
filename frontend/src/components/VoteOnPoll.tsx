import { useState, type FC, type FormEvent, useEffect } from "react";
import styles from "../App.module.css";
import type { Poll as PollData } from "../api";
import { useAuth } from "../Auth";
import { castVote, getPollResults } from "../api";
import { onWs } from "../ws";

interface VoteOnPollProps {
  pollData: PollData;
}

const VoteOnPoll: FC<VoteOnPollProps> = ({ pollData }) => {
  const { currentUser } = useAuth();
  const [selectedOptions, setSelectedOptions] = useState<string[]>([]);
  const [results, setResults] = useState<Record<string, number>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  useEffect(() => {
    const off = onWs((msg) => {
      try {
        if (typeof msg !== "object" || msg === null) return;

        const payload = msg as any;
        if (payload?.type === "vote-delta") {
          const pollIdNum = Number(payload.pollId);
          if (pollIdNum === pollData.pollId) {
            void loadResults();
          }
        }
      } catch {
        void loadResults();
      }
    });

    return () => {
      off();
    };
  }, [pollData.pollId]);

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
    setIsSubmitting(true);
    try {
      const votePromises = selectedOptions.map((caption) => {
        const optionToVote = pollData.pollOptions.find(
          (opt) => opt.caption === caption
        );
        if (!optionToVote) throw new Error(`Option ${caption} not found`);
        return castVote(
          pollData.pollId,
          optionToVote.presentationOrder,
          currentUser?.id
        );
      });
      await Promise.all(votePromises);
    } catch (error) {
      console.error(error);
      alert("Failed to submit vote");
    } finally {
      setIsSubmitting(false);
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
                />
              </div>
            </div>
          );
        })}
      </div>

      <form onSubmit={handleSubmit} style={{ marginTop: "20px" }}>
        <p>
          <strong>Your Vote:</strong>
        </p>
        {pollData.pollOptions.map((option) => (
          <label
            key={option.presentationOrder}
            className={`${styles.voteOption} ${
              selectedOptions.includes(option.caption)
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
        ))}
        <button
          type="submit"
          className={styles.submitButton}
          style={{ marginTop: "20px" }}
          disabled={isSubmitting}
        >
          {isSubmitting ? "Submitting..." : "Submit Vote"}
        </button>
      </form>
    </div>
  );
};

export default VoteOnPoll;
