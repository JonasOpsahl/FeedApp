import { useState, type FC, type FormEvent } from "react";
import styles from "../App.module.css";
import { createPoll } from "../api";
import { useAuth } from "../Auth";

interface CreatePollProps {
  onPollCreated: () => void;
}

const CreatePoll: FC<CreatePollProps> = ({ onPollCreated }) => {
  const { currentUser } = useAuth();
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState(["", ""]);
  const [votesPerUser, setVotesPerUser] = useState(1);
  const [duration, setDuration] = useState(1);
  const [isPublic, setIsPublic] = useState(true);
  const [userList, setUserList] = useState("");

  const handleOptionChange = (index: number, value: string) => {
    const newOptions = [...options];
    newOptions[index] = value;
    setOptions(newOptions);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!currentUser) {
      alert("You must be logged in to create a poll.");
      return;
    }

    const filteredOptions = options.filter((opt) => opt.trim() !== "");
    if (!question.trim() || filteredOptions.length < 2 || votesPerUser < 1) {
      alert("Please fill out all fields correctly.");
      return;
    }

    try {
      await createPoll({
        question,
        durationDays: duration,
        creatorId: currentUser.id,
        visibility: isPublic ? "PUBLIC" : "PRIVATE",
        maxVotesPerUser: votesPerUser,
        invitedUsers: isPublic
          ? []
          : userList
              .split(/[\s,]+/)
              .map((id) => parseInt(id.trim(), 10))
              .filter((num) => !isNaN(num)),
        optionCaptions: filteredOptions,
        optionOrders: filteredOptions.map((_, index) => index + 1),
      });

      alert("Poll created successfully!");
      onPollCreated();

      setQuestion("");
      setOptions(["", ""]);
      setVotesPerUser(1);
      setDuration(1);
      setIsPublic(true);
      setUserList("");
    } catch (error) {
      console.error(error);
      alert("Failed to create poll.");
    }
  };

  return (
    <div>
      <h2>Poll Creation</h2>
      <form onSubmit={handleSubmit}>
        <div className={styles.formGroup}>
          <label htmlFor="question" className={styles.label}>
            Poll Question
          </label>
          <input
            type="text"
            id="question"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            className={styles.baseInput}
            required
          />
        </div>
        <div className={styles.formGroup}>
          <label className={styles.label}>Vote Options</label>
          {options.map((option, index) => (
            <div key={index} className={styles.optionContainer}>
              <input
                type="text"
                value={option}
                onChange={(e) => handleOptionChange(index, e.target.value)}
                className={styles.optionInput}
                required
              />
              <button
                type="button"
                onClick={() =>
                  setOptions(options.filter((_, i) => i !== index))
                }
                className={styles.removeButton}
                disabled={options.length <= 2}
              >
                Remove
              </button>
            </div>
          ))}
          <button
            type="button"
            onClick={() => setOptions([...options, ""])}
            className={styles.addButton}
          >
            Add Option
          </button>
        </div>
        <div className={styles.formGroup}>
          <label htmlFor="votesPerUser" className={styles.label}>
            Max votes per user
          </label>
          <input
            type="number"
            id="votesPerUser"
            value={votesPerUser}
            onChange={(e) => setVotesPerUser(Number(e.target.value))}
            className={styles.baseInput}
            min="1"
            required
          />
        </div>
        <div className={styles.formGroup}>
          <label htmlFor="duration" className={styles.label}>
            Poll Duration (Days)
          </label>
          <select
            id="duration"
            value={duration}
            onChange={(e) => setDuration(Number(e.target.value))}
            className={styles.baseInput}
          >
            <option value={1}>1 Day</option>
            <option value={3}>3 Days</option>
            <option value={7}>1 Week</option>
            <option value={30}>30 Days</option>
          </select>
        </div>
        <div className={styles.formGroup}>
          <div className={styles.checkboxContainer}>
            <input
              type="checkbox"
              id="isPublic"
              checked={isPublic}
              onChange={(e) => setIsPublic(e.target.checked)}
            />
            <label htmlFor="isPublic">Should poll be public?</label>
          </div>
        </div>
        {!isPublic && (
          <div className={styles.formGroup}>
            <label htmlFor="userList" className={styles.label}>
              Invited User IDs
            </label>
            <p>
              Enter list of users to be invited (ID's). Can be separated by comma, space or new line.
            </p>
            <textarea
              id="userList"
              value={userList}
              onChange={(e) => setUserList(e.target.value)}
              className={styles.baseInput}
              rows={4}
              placeholder="Ex: 1, 2, 3"
            />
          </div>
        )}
        <button type="submit" className={styles.submitButton}>
          Create Poll
        </button>
      </form>
    </div>
  );
};

export default CreatePoll;
