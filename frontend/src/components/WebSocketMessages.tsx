import React from "react";

export default function WebSocketMessages({ messages }: { messages: string[] }) {
  return (
    <div style={{
      borderRadius: 16, padding: 14, marginBottom: 16,
      background: "linear-gradient(90deg,#1f2a38,#242a30)",
      border: "1px solid rgba(255,255,255,0.12)", color: "#cde8ff"
    }}>
      <div style={{ fontSize: 18, fontWeight: 600, color: "#5bd1ff", marginBottom: 8 }}>
        WebSocket Messages
      </div>
      <div style={{
        display: "inline-block", padding: "6px 12px",
        borderRadius: 999, border: "1px solid rgba(255,255,255,0.18)",
        background: "rgba(255,255,255,0.07)"
      }}>
        {messages[0] ?? "—"}
      </div>
    </div>
  );
}