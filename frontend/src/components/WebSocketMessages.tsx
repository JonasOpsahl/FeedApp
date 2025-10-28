import styles from "../App.module.css";

export default function WebSocketMessages({ messages }: { messages: string[] }) {
  return (
    <div className={styles.wsPanel}>
      <div className={styles.wsTitle}>WebSocket Messages</div>
      <div className={styles.wsChipRow}>
        <div className={styles.wsChip}>{messages[0] ?? "—"}</div>
      </div>
    </div>
  );
}