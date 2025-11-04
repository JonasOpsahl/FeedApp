import { useState, type FC, type FormEvent, useEffect } from "react";
import styles from "../App.module.css";
import type { Poll as PollData } from "../api";
import { useAuth } from "../Auth";
import { castVote, getPollResults } from "../api";
import { onWs } from "../ws";
import confetti from "canvas-confetti"; 
import EditPollModal from "./ManagePolls";


interface VoteOnPollProps {
  pollData: PollData;
}

const toDeadlineDate = (p: PollData): Date | null => {
  const anyP = p as any;
  const v = anyP.validUntil ?? anyP.deadline ?? anyP.expiresAt;
  if (!v) return null;
  if (typeof v === "number") return new Date(v);
  if (v instanceof Date) return v;
  if (typeof v === "string") {
    const asNum = Number(v);
    if (Number.isFinite(asNum)) return new Date(asNum);
    const parsed = Date.parse(v);
    if (!Number.isNaN(parsed)) return new Date(parsed);
  }
  return null;
};

const formatDeadline = (d: Date) =>
  new Intl.DateTimeFormat("no-NO", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(d);


const VoteOnPoll: FC<VoteOnPollProps & { onChanged?: () => void }> = ({pollData, onChanged,}) => {
  const { currentUser } = useAuth();
  const isOwner = !!currentUser && ((pollData as any).creatorId === currentUser.id || (pollData as any).creator?.id === currentUser.id);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [selectedOptions, setSelectedOptions] = useState<string[]>([]);
  const [results, setResults] = useState<Record<string, number>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

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
      const results = await Promise.all(votePromises);
      if (results.some((r) => r === true)) {
        fireConfetti();
      }
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

  const deadline = toDeadlineDate(pollData);
  const formattedDeadline = deadline ? formatDeadline(deadline) : null;

 return (
    <div className={styles.pollCard} style={{ position: "relative" }}>
      {isOwner && (
        <button
          className={styles.editButton}
          style={{ position: "absolute", top: 8, right: 8 }}
          title="Edit poll"
          onClick={() => setIsEditOpen(true)}
        >
          Edit
        </button>
      )}

      <h3>{pollData.question}</h3>

      <div className={styles.optionsList}>
        {pollData.pollOptions.map((option) => {
          const voteCount = results[option.caption] || 0;
          const percentage = totalVotes > 0 ? (voteCount / totalVotes) * 100 : 0;
          return (
            <div key={option.presentationOrder} className={styles.voteResultRow}>
              <div className={styles.voteOptionLabel}>
                <span>{option.caption}</span>
                <strong>
                  {voteCount} vote(s) ({percentage.toFixed(1)}%)
                </strong>
              </div>
              <div className={styles.progressBarContainer}>
                <div className={styles.progressBar} style={{ width: `${percentage}%` }} />
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
      {formattedDeadline && (
        <div className={styles.deadline}>DEADLINE: {formattedDeadline}</div>
      )}
      {isEditOpen && currentUser && (
        <EditPollModal
          poll={pollData}
          ownerUserId={currentUser.id}
          onClose={() => setIsEditOpen(false)}
          onChanged={() => {
            setIsEditOpen(false);
            onChanged?.();
          }}
        />
      )}
    </div>
  );
};

export default VoteOnPoll;
