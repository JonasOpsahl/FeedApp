export type WsEvent =
  | { type: 'poll-created'; pollId: number; ts: number }
  | { type: 'poll-deleted'; pollId: number; ts: number }
  | { type: 'vote-delta'; pollId: number; optionOrder: number; voterUserId: number | null; ts: number }
  | { type: "poll-updated"; pollId: number; ts: number }; 


export type Listener = (e: WsEvent | string) => void;

const listeners = new Set<Listener>();

export function onWs(fn: Listener) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

let sock: WebSocket | null = null;
let tries = 0;

function url() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${location.hostname}:8080/rawws`;
}

export function connectWs() {
  if (sock && (sock.readyState === WebSocket.OPEN || sock.readyState === WebSocket.CONNECTING)) return;

  sock = new WebSocket(url());

  sock.onopen = () => {
    tries = 0;
    sock?.send('Hello from React frontend!');
  };

  sock.onmessage = (ev) => {
    const raw = ev.data;
    try {
      const e = JSON.parse(raw) as WsEvent;
      if (e && typeof e === 'object' && 'type' in e) {
        listeners.forEach(l => l(e));
        return;
      }
    } catch {}
    listeners.forEach(l => l(String(raw)));
  };

  sock.onclose = () => {
    sock = null;
    const backoff = Math.min(1000 * 2 ** tries++, 10000);
    setTimeout(connectWs, backoff);
  };

  sock.onerror = () => sock?.close();
}

