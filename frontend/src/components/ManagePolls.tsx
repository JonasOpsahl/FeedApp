import { useMemo, useState } from "react";
import styles from "../App.module.css";
import type { Poll } from "../api";
import { addOptionsToPoll, deletePoll, updatePollDurationDays } from "../api";

interface Props {
  poll: Poll;
  ownerUserId: number;
  onClose: () => void;
  onChanged: () => void;
}

const toIsoLocal = (d: Date) => {
  const pad = (n: number) => `${n}`.padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
const readDeadlineMs = (p: Poll): number => {
  const anyP = p as any;
  const v = anyP.validUntil ?? anyP.deadline ?? anyP.expiresAt;
  return typeof v === "number" ? v :
         typeof v === "string" ? Number(v) || Date.parse(v) :
         v instanceof Date ? v.getTime() : Date.now() + 24*3600_000;
};

export default function EditPollModal({ poll, ownerUserId, onClose, onChanged }: Props) {
  const nextOrder = useMemo(
    () => Math.max(...poll.pollOptions.map(o => o.presentationOrder)) + 1,
    [poll.pollOptions]
  );

  const [deadline, setDeadline] = useState<string>(toIsoLocal(new Date(readDeadlineMs(poll))));
  const [newOptions, setNewOptions] = useState<string[]>([""]);
  const [busy, setBusy] = useState(false);

  const setOption = (i: number, v: string) => {
    setNewOptions(prev => prev.map((x, idx) => idx === i ? v : x));
  };

  const addBlank = () => setNewOptions(prev => [...prev, ""]);
  const removeAt = (i: number) => setNewOptions(prev => prev.filter((_, idx) => idx !== i));

  const handleSave = async () => {
    setBusy(true);
    try {
      // 1) Add options (if any non-empty)
      const toAdd = newOptions.map(s => s.trim()).filter(Boolean);
      if (toAdd.length > 0) {
        const orders = toAdd.map((_, i) => nextOrder + i);
        await addOptionsToPoll(poll.pollId, ownerUserId, toAdd, orders);
      }
      // 2) Update deadline by delta from current validUntil
      const currentMs = readDeadlineMs(poll);
      const targetMs = new Date(deadline).getTime();
      const deltaDays = Math.ceil((targetMs - currentMs) / 86_400_000);
      if (Number.isFinite(deltaDays) && deltaDays !== 0) {
        await updatePollDurationDays(poll.pollId, ownerUserId, deltaDays);
      }
      onChanged();
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("Delete this poll permanently?")) return;
    setBusy(true);
    try {
      await deletePoll(poll.pollId, ownerUserId);
      onChanged();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <h3>Edit Poll #{poll.pollId}</h3>
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>Deadline</label>
          <input
            type="datetime-local"
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
            className={styles.baseInput}
          />
        </div>

        <div className={styles.formGroup}>
          <label className={styles.label}>Add Options</label>
          {newOptions.map((opt, i) => (
            <div key={i} className={styles.optionContainer}>
              <input
                className={styles.optionInput}
                value={opt}
                onChange={(e) => setOption(i, e.target.value)}
                placeholder={`New option #${i + 1}`}
              />
              <button
                type="button"
                className={styles.removeButton}
                onClick={() => removeAt(i)}
                disabled={newOptions.length <= 1}
              >
                Remove
              </button>
            </div>
          ))}
          <button type="button" className={styles.baseButton} onClick={addBlank}>
            Add another
          </button>
        </div>

        <div className={styles.modalActions}>
          <button className={styles.removeButton} onClick={handleDelete} disabled={busy}>Delete Poll</button>
          <div style={{ flex: 1 }} />
          <button className={styles.baseButton} onClick={onClose}>Cancel</button>
          <button className={styles.submitButton} onClick={handleSave} disabled={busy}>Save</button>
        </div>
      </div>
    </div>
  );
}