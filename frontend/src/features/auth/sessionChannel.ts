export type SessionSignalKind = "login" | "logout";

export type SessionSignal = {
  id: string;
  kind: SessionSignalKind;
  source: string;
  issuedAt: number;
};

export type SessionChannel = {
  publish: (kind: SessionSignalKind) => void;
  close: () => void;
};

export const SESSION_SIGNAL_STORAGE_KEY = "rental_auth_session_signal";

const CHANNEL_NAME = "rental-management-auth-session";

export function createSessionChannel(onSignal: (signal: SessionSignal) => void): SessionChannel {
  const source = createId();

  if (typeof BroadcastChannel !== "undefined") {
    const channel = new BroadcastChannel(CHANNEL_NAME);
    channel.addEventListener("message", (event: MessageEvent<unknown>) => {
      const signal = parseSignal(event.data);
      if (signal && signal.source !== source) {
        onSignal(signal);
      }
    });

    return {
      publish(kind) {
        channel.postMessage(createSignal(kind, source));
      },
      close() {
        channel.close();
      },
    };
  }

  const handleStorage = (event: StorageEvent) => {
    if (event.key !== SESSION_SIGNAL_STORAGE_KEY || !event.newValue) {
      return;
    }

    const signal = parseSignal(event.newValue);
    if (signal && signal.source !== source) {
      onSignal(signal);
    }
  };

  window.addEventListener("storage", handleStorage);

  return {
    publish(kind) {
      const serialized = JSON.stringify(createSignal(kind, source));
      try {
        // Storage is used only as a transient notification bus. The payload
        // contains no token, cookie, profile, email, or other account data.
        window.localStorage.setItem(SESSION_SIGNAL_STORAGE_KEY, serialized);
        window.localStorage.removeItem(SESSION_SIGNAL_STORAGE_KEY);
      } catch {
        // Browsers may disable storage. The current tab still completes its
        // local session transition; other tabs restore on their next request.
      }
    },
    close() {
      window.removeEventListener("storage", handleStorage);
    },
  };
}

function createSignal(kind: SessionSignalKind, source: string): SessionSignal {
  return {
    id: createId(),
    kind,
    source,
    issuedAt: Date.now(),
  };
}

function parseSignal(value: unknown): SessionSignal | null {
  try {
    const candidate = typeof value === "string" ? JSON.parse(value) as unknown : value;
    if (!candidate || typeof candidate !== "object") {
      return null;
    }

    const signal = candidate as Partial<SessionSignal>;
    if (
      typeof signal.id !== "string" ||
      (signal.kind !== "login" && signal.kind !== "logout") ||
      typeof signal.source !== "string" ||
      typeof signal.issuedAt !== "number"
    ) {
      return null;
    }

    return {
      id: signal.id,
      kind: signal.kind,
      source: signal.source,
      issuedAt: signal.issuedAt,
    };
  } catch {
    return null;
  }
}

function createId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
