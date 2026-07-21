import { isValidSessionGeneration } from "./sessionGeneration";

export type SessionSignalKind = "login" | "logout" | "logout-complete";

export type SessionSignal = {
  id: string;
  generation: string;
  kind: SessionSignalKind;
  source: string;
  issuedAt: number;
};

export type SessionChannel = {
  publish: (kind: SessionSignalKind, generation: string) => void;
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
      publish(kind, generation) {
        channel.postMessage(createSignal(kind, source, generation));
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
    publish(kind, generation) {
      const serialized = JSON.stringify(createSignal(kind, source, generation));
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

function createSignal(kind: SessionSignalKind, source: string, generation: string): SessionSignal {
  if (!isValidSessionGeneration(generation)) {
    throw new Error("Invalid session generation");
  }
  return {
    id: createId(),
    generation,
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
      !isValidSessionGeneration(signal.generation) ||
      (signal.kind !== "login" && signal.kind !== "logout" && signal.kind !== "logout-complete") ||
      typeof signal.source !== "string" ||
      typeof signal.issuedAt !== "number"
    ) {
      return null;
    }

    return {
      id: signal.id,
      generation: signal.generation,
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
