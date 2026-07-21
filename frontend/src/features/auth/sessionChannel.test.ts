import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createSessionChannel,
  SESSION_SIGNAL_STORAGE_KEY,
  type SessionSignal,
} from "./sessionChannel";

const originalBroadcastChannel = globalThis.BroadcastChannel;
const GENERATION_A = "generation-a-123456";

class FakeBroadcastChannel {
  static instances: FakeBroadcastChannel[] = [];

  readonly name: string;
  readonly posted: unknown[] = [];
  private listener: ((event: MessageEvent<unknown>) => void) | null = null;

  constructor(name: string) {
    this.name = name;
    FakeBroadcastChannel.instances.push(this);
  }

  addEventListener(_type: string, listener: (event: MessageEvent<unknown>) => void) {
    this.listener = listener;
  }

  postMessage(message: unknown) {
    this.posted.push(message);
  }

  emit(message: unknown) {
    this.listener?.(new MessageEvent("message", { data: message }));
  }

  close() {
    this.listener = null;
  }
}

afterEach(() => {
  Object.defineProperty(globalThis, "BroadcastChannel", {
    configurable: true,
    value: originalBroadcastChannel,
  });
  FakeBroadcastChannel.instances = [];
  window.localStorage.clear();
  vi.restoreAllMocks();
});

describe("sessionChannel", () => {
  it("broadcasts every session transition without any credential or user data", () => {
    Object.defineProperty(globalThis, "BroadcastChannel", {
      configurable: true,
      value: FakeBroadcastChannel,
    });
    const listener = vi.fn();
    const channel = createSessionChannel(listener);
    const transport = FakeBroadcastChannel.instances[0];

    channel.publish("login", GENERATION_A);
    channel.publish("logout", GENERATION_A);
    channel.publish("logout-complete", GENERATION_A);

    expect(transport.name).toBe("rental-management-auth-session");
    expect(transport.posted).toHaveLength(3);
    expect(transport.posted.map((message) => (message as SessionSignal).kind)).toEqual([
      "login",
      "logout",
      "logout-complete",
    ]);
    transport.posted.forEach((message) => {
      expect(Object.keys(message as object).sort()).toEqual([
        "generation",
        "id",
        "issuedAt",
        "kind",
        "source",
      ]);
    });
    const serialized = JSON.stringify(transport.posted).toLowerCase();
    expect(serialized).not.toContain("token");
    expect(serialized).not.toContain("user");
    expect(serialized).not.toContain("email");
    expect(serialized).not.toContain("account");
    channel.close();
  });

  it("accepts valid remote signals but ignores malformed and self-originated messages", () => {
    Object.defineProperty(globalThis, "BroadcastChannel", {
      configurable: true,
      value: FakeBroadcastChannel,
    });
    const listener = vi.fn();
    const channel = createSessionChannel(listener);
    const transport = FakeBroadcastChannel.instances[0];

    channel.publish("login", GENERATION_A);
    const localSignal = transport.posted[0] as SessionSignal;
    transport.emit(localSignal);
    transport.emit({ kind: "logout", token: "must-not-be-accepted" });
    transport.emit({
      id: "remote-logout",
      generation: GENERATION_A,
      kind: "logout",
      source: "another-tab",
      issuedAt: Date.now(),
    } satisfies SessionSignal);

    expect(listener).toHaveBeenCalledOnce();
    expect(listener).toHaveBeenCalledWith(expect.objectContaining({ kind: "logout" }));
    channel.close();
  });

  it("uses a transient token-free storage signal when BroadcastChannel is unavailable", () => {
    Object.defineProperty(globalThis, "BroadcastChannel", {
      configurable: true,
      value: undefined,
    });
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    const channel = createSessionChannel(vi.fn());

    channel.publish("logout", GENERATION_A);

    expect(setItem).toHaveBeenCalledOnce();
    const [key, serialized] = setItem.mock.calls[0];
    expect(key).toBe(SESSION_SIGNAL_STORAGE_KEY);
    expect(JSON.parse(serialized)).toMatchObject({
      kind: "logout",
      generation: GENERATION_A,
    });
    expect(serialized.toLowerCase()).not.toMatch(/token|user|email|account/);
    expect(window.localStorage.getItem(SESSION_SIGNAL_STORAGE_KEY)).toBeNull();
    channel.close();
  });
});
