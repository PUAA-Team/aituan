import fs from 'node:fs';
import path from 'node:path';
import { performance } from 'node:perf_hooks';

const origin = (process.env.LOAD_ORIGIN || 'http://api-gateway:8080').replace(/\/$/, '');
const account = process.env.LOAD_ACCOUNT || 'demo_user';
const password = process.env.LOAD_PASSWORD || '123456';
const durationSeconds = positiveInteger(process.env.LOAD_DURATION_SECONDS, 180);
const concurrency = positiveInteger(process.env.LOAD_CONCURRENCY, 80);
const outputDirectory = process.env.LOAD_OUTPUT_DIR || '/results';
const requestTimeoutMs = positiveInteger(process.env.LOAD_REQUEST_TIMEOUT_MS, 5000);

const targets = [
  {name: 'identity-asset-service', path: '/api/app/account/profile'},
  {name: 'merchant-catalog-service', path: '/api/app/discovery/home'},
  {name: 'trade-fulfillment-service', path: '/api/app/trade/orders?page=1&pageSize=20'},
  {name: 'engagement-platform-service', path: '/api/app/interaction/stores/1/reviews?page=1&pageSize=20'},
];

fs.mkdirSync(outputDirectory, {recursive: true});
const requestLog = fs.createWriteStream(path.join(outputDirectory, 'requests.jsonl'), {flags: 'w'});
const perTarget = new Map(targets.map(({name}) => [name, {count: 0, ok: 0, errors: 0, latencies: []}]));
let sequence = 0;

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function percentile(sorted, ratio) {
  if (sorted.length === 0) return 0;
  return sorted[Math.min(sorted.length - 1, Math.ceil(sorted.length * ratio) - 1)];
}

function summarize(stats, elapsedSeconds) {
  const latencies = [...stats.latencies].sort((a, b) => a - b);
  const totalLatency = latencies.reduce((sum, value) => sum + value, 0);
  return {
    requests: stats.count,
    successes: stats.ok,
    errors: stats.errors,
    errorRate: stats.count === 0 ? 1 : stats.errors / stats.count,
    throughputRps: elapsedSeconds === 0 ? 0 : stats.count / elapsedSeconds,
    averageMs: stats.count === 0 ? 0 : totalLatency / stats.count,
    p50Ms: percentile(latencies, 0.50),
    p95Ms: percentile(latencies, 0.95),
    p99Ms: percentile(latencies, 0.99),
    maxMs: latencies.at(-1) ?? 0,
  };
}

async function login() {
  const response = await fetch(`${origin}/api/open/auth/user/login/password`, {
    method: 'POST',
    headers: {'content-type': 'application/json'},
    body: JSON.stringify({account, password}),
    signal: AbortSignal.timeout(requestTimeoutMs),
  });
  const payload = await response.json();
  const token = payload?.data?.accessToken || payload?.data?.token;
  if (!response.ok || payload?.code !== 0 || !token) {
    throw new Error(`login failed: http=${response.status}, code=${payload?.code ?? 'unknown'}`);
  }
  return token;
}

async function writeLine(value) {
  if (!requestLog.write(`${JSON.stringify(value)}\n`)) {
    await new Promise((resolve) => requestLog.once('drain', resolve));
  }
}

async function worker(workerId, token, stopAt) {
  while (Date.now() < stopAt) {
    const requestId = sequence++;
    const target = targets[requestId % targets.length];
    const stats = perTarget.get(target.name);
    const startedAt = new Date().toISOString();
    const started = performance.now();
    let httpStatus = 0;
    let apiCode = null;
    let error = null;
    let ok = false;
    try {
      const response = await fetch(`${origin}${target.path}`, {
        headers: {
          authorization: `Bearer ${token}`,
          'x-request-id': `hpa-${workerId}-${requestId}`,
        },
        signal: AbortSignal.timeout(requestTimeoutMs),
      });
      httpStatus = response.status;
      const payload = await response.json();
      apiCode = payload?.code ?? null;
      ok = response.ok && apiCode === 0;
      if (!ok) error = payload?.message || `HTTP ${response.status}`;
    } catch (caught) {
      error = caught instanceof Error ? caught.message : String(caught);
    }
    const latencyMs = Number((performance.now() - started).toFixed(3));
    stats.count += 1;
    stats.latencies.push(latencyMs);
    if (ok) stats.ok += 1;
    else stats.errors += 1;
    await writeLine({startedAt, workerId, requestId, target: target.name, path: target.path, latencyMs, httpStatus, apiCode, ok, error});
  }
}

const startedAt = new Date();
console.log(JSON.stringify({event: 'load-start', startedAt: startedAt.toISOString(), origin, durationSeconds, concurrency, targets: targets.map(({name, path: targetPath}) => ({name, path: targetPath}))}));

try {
  const token = await login();
  const stopAt = Date.now() + durationSeconds * 1000;
  await Promise.all(Array.from({length: concurrency}, (_, index) => worker(index, token, stopAt)));
  await new Promise((resolve, reject) => requestLog.end((error) => error ? reject(error) : resolve()));

  const endedAt = new Date();
  const elapsedSeconds = (endedAt.getTime() - startedAt.getTime()) / 1000;
  const overallStats = {count: 0, ok: 0, errors: 0, latencies: []};
  const byTarget = {};
  for (const [name, stats] of perTarget.entries()) {
    overallStats.count += stats.count;
    overallStats.ok += stats.ok;
    overallStats.errors += stats.errors;
    overallStats.latencies.push(...stats.latencies);
    byTarget[name] = summarize(stats, elapsedSeconds);
  }
  const summary = {
    startedAt: startedAt.toISOString(),
    endedAt: endedAt.toISOString(),
    elapsedSeconds,
    config: {origin, durationSeconds, concurrency, requestTimeoutMs, account, targetCount: targets.length},
    overall: summarize(overallStats, elapsedSeconds),
    byTarget,
  };
  fs.writeFileSync(path.join(outputDirectory, 'load-summary.json'), `${JSON.stringify(summary, null, 2)}\n`);
  console.log(JSON.stringify({event: 'load-finish', summary}));
  if (summary.overall.requests === 0 || summary.overall.errorRate > 0.05) process.exitCode = 1;
} catch (error) {
  requestLog.end();
  console.error(error instanceof Error ? error.stack : String(error));
  process.exitCode = 1;
}
