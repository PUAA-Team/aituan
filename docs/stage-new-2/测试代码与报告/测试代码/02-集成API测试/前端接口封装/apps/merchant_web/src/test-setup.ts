import { Storage } from 'happy-dom';

// Node 25 may expose an incomplete experimental localStorage before happy-dom initializes.
Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: new Storage(),
});
