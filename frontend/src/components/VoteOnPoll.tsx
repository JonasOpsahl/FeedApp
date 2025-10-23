import { useState, type FC, type FormEvent, useEffect } from "react";
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
      setResults(pollResults);
    } catch (error) {
      console.error("Could not fetch poll results", error);
    }
  };
  
  useEffect(() => {
    void loadResults();
  }, [pollData.pollId]);

  // Subscribe to WebSocket for this poll
  useEffect(() => {
    connectWs();
    const off = onWs((msg) => {
      if (typeof msg === "string") {
        if (msg === "votesUpdated") void loadResults();
        return;
      }
      if (msg.type === "vote-delta" && Number(msg.pollId) === Number(pollData.pollId)) {
        void loadResults();
      }
    });
    return () => {
      // call unsubscribe and ignore its return value so cleanup returns void
      off();
    };
  }, [pollData.pollId]);


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
          throw new Error(`Option ${optionCaption} not found`); // Needed to make sure that optionToVote is actually present
        }

        return castVote(
          pollData.pollId,
          optionToVote.presentationOrder,
          currentUser?.id
        );
      });

      await Promise.all(votePromises);
      setHasVoted(true);
      const updatedResults = await getPollResults(pollData.pollId);
      setResults(updatedResults);

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
