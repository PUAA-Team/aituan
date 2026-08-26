import { mkdirSync } from 'node:fs';

export default function globalSetup() {
  mkdirSync('test-results/artifacts', { recursive: true });
}
