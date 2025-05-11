type EventCallback = (...args: any[]) => void;

interface EventBus {
  on(event: string, callback: EventCallback): void;
  off(event: string, callback: EventCallback): void;
  emit(event: string, ...args: any[]): void;
}

class EventBusImpl implements EventBus {
  private events: Record<string, EventCallback[]> = {};

  public on(event: string, callback: EventCallback): void {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  }

  public off(event: string, callback: EventCallback): void {
    if (!this.events[event]) return;
    this.events[event] = this.events[event].filter(cb => cb !== callback);
  }

  public emit(event: string, ...args: any[]): void {
    if (!this.events[event]) return;
    this.events[event].forEach(callback => callback(...args));
  }
}

// Singleton instance
const eventBus = new EventBusImpl();

// Event names constants
export const EVENTS = {
  THREAD_FOLLOWED: 'THREAD_FOLLOWED',
  THREAD_UNFOLLOWED: 'THREAD_UNFOLLOWED',
  THREAD_VIEWED: 'THREAD_VIEWED'
};

export default eventBus; 