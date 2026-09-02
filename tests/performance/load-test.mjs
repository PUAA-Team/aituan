#!/usr/bin/env node

import { execFileSync, spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import process from 'node:process';
import { performance } from 'node:perf_hooks';

const DEFAULTS = {
  concurrency: 50,
  requests: 15000,
  runs: 3,
  timeoutMs: 5000,
  warmupRequests: 1000,
  resourceIntervalMs: 500,
  cooldownMs: 0,
};

function usage() {
  return `统一性能压测脚本

用法：
  node tests/performance/load-test.mjs \\
    --target monolith=http://127.0.0.1:18080 \\
    --pids monolith=12345 \\
    --runs 3 --concurrency 50 --requests 15000

可重复传入 --target 和 --pids。所有目标按相同场景和参数顺序执行。
可选参数：--scenarios、--output、--timeout-ms、--warmup-requests、--resource-interval-ms、--cooldown-ms、--proc-root。
`;
}

function parsePositiveInteger(value, option) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${option} 必须是正整数，实际为 ${value}`);
  }
  return parsed;
}

function parseLabelValue(value, option) {
  const separator = value.indexOf('=');
  if (separator <= 0 || separator === value.length - 1) {
    throw new Error(`${option} 格式应为 label=value，实际为 ${value}`);
  }
  const label = value.slice(0, separator).trim();
  const data = value.slice(separator + 1).trim();
  if (!/^[a-zA-Z0-9_-]+$/.test(label)) {
    throw new Error(`目标标签只能包含字母、数字、下划线或连字符：${label}`);
  }
  return [label, data];
}

function parseArgs(argv) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const options = {
    ...DEFAULTS,
    targets: [],
    pids: new Map(),
    procRoot: '/proc',
    scenariosPath: path.resolve('tests/performance/scenarios.json'),
    outputPath: path.resolve(`tests/performance/results/${timestamp}`),
  };

  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    const value = argv[index + 1];
    if (option === '--help' || option === '-h') {
      options.help = true;
      continue;
    }
    if (!value || value.startsWith('--')) {
      throw new Error(`${option} 缺少参数值`);
    }
    switch (option) {
      case '--target': {
        const [label, baseUrl] = parseLabelValue(value, option);
        options.targets.push({ label, baseUrl: new URL(baseUrl).toString().replace(/\/$/, '') });
        break;
      }
      case '--pids': {
        const [label, rawPids] = parseLabelValue(value, option);
        const pids = rawPids.split(',').map((pid) => parsePositiveInteger(pid.trim(), option));
        options.pids.set(label, pids);
        break;
      }
      case '--runs':
        options.runs = parsePositiveInteger(value, option);
        break;
      case '--concurrency':
        options.concurrency = parsePositiveInteger(value, option);
        break;
      case '--requests':
        options.requests = parsePositiveInteger(value, option);
        break;
      case '--timeout-ms':
        options.timeoutMs = parsePositiveInteger(value, option);
        break;
      case '--warmup-requests':
        options.warmupRequests = parsePositiveInteger(value, option);
        break;
      case '--resource-interval-ms':
        options.resourceIntervalMs = parsePositiveInteger(value, option);
        break;
      case '--cooldown-ms':
        options.cooldownMs = parsePositiveInteger(value, option);
        break;
      case '--proc-root':
        options.procRoot = path.resolve(value);
        break;
      case '--scenarios':
        options.scenariosPath = path.resolve(value);
        break;
      case '--output':
        options.outputPath = path.resolve(value);
        break;
      default:
        throw new Error(`未知参数：${option}`);
    }
    index += 1;
  }

  if (!options.help && options.targets.length === 0) {
    throw new Error('至少需要一个 --target label=http://host:port');
  }
  const labels = new Set();
  for (const target of options.targets) {
    if (labels.has(target.label)) {
      throw new Error(`目标标签重复：${target.label}`);
    }
    labels.add(target.label);
  }
  return options;
}

function getJsonPath(value, expression) {
  if (!expression.startsWith('$.')) {
    throw new Error(`仅支持 $.field 形式的 JSON 路径：${expression}`);
  }
  return expression.slice(2).split('.').reduce((current, key) => current?.[key], value);
}

function percentile(sortedValues, ratio) {
  if (sortedValues.length === 0) return 0;
  const index = Math.max(0, Math.ceil(sortedValues.length * ratio) - 1);
  return sortedValues[index];
}

function round(value, digits = 3) {
  const factor = 10 ** digits;
  return Math.round(value * factor) / factor;
}

function shellVersion(command, args) {
  const result = spawnSync(command, args, { encoding: 'utf8' });
  if (result.error) return null;
  const output = `${result.stdout ?? ''}${result.stderr ?? ''}`.trim();
  return output || null;
}

function machineInfo() {
  const cpus = os.cpus();
  return {
    capturedAt: new Date().toISOString(),
    hostname: os.hostname(),
    platform: os.platform(),
    release: os.release(),
    architecture: os.arch(),
    cpuModel: cpus[0]?.model ?? 'unknown',
    logicalCpuCount: cpus.length,
    totalMemoryBytes: os.totalmem(),
    totalMemoryGiB: round(os.totalmem() / 1024 ** 3),
    nodeVersion: process.version,
    javaVersion: shellVersion('java', ['-version']),
    mavenVersion: shellVersion('mvn', ['-version']),
    gitCommit: shellVersion('git', ['rev-parse', 'HEAD']),
  };
}

let linuxClockTicks;

function parseLinuxProcessSample(pids, procRoot) {
  linuxClockTicks ??= Number(execFileSync('getconf', ['CLK_TCK'], { encoding: 'utf8' }).trim()) || 100;
  return pids.flatMap((pid) => {
    try {
      const stat = readFileSync(path.join(procRoot, String(pid), 'stat'), 'utf8');
      const fields = stat.slice(stat.lastIndexOf(')') + 2).trim().split(/\s+/);
      const status = readFileSync(path.join(procRoot, String(pid), 'status'), 'utf8');
      const rssMatch = status.match(/^VmRSS:\s+(\d+)\s+kB$/m);
      return [{
        pid,
        cpuSeconds: (Number(fields[11]) + Number(fields[12])) / linuxClockTicks,
        memoryBytes: Number(rssMatch?.[1] ?? 0) * 1024,
      }];
    } catch {
      return [];
    }
  });
}

function parseUnixProcessSample(pids) {
  const output = execFileSync('ps', ['-o', 'pid=,%cpu=,rss=', '-p', pids.join(',')], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  });
  const processes = output.trim().split('\n').filter(Boolean).map((line) => {
    const [pid, cpuPercent, rssKiB] = line.trim().split(/\s+/);
    return { pid: Number(pid), cpuPercent: Number(cpuPercent), memoryBytes: Number(rssKiB) * 1024 };
  });
  return processes;
}

function parseWindowsProcessSample(pids) {
  const safePids = pids.filter(Number.isInteger).join(',');
  const script = [
    `$items = Get-Process -Id ${safePids} -ErrorAction SilentlyContinue`,
    '$items | Select-Object Id,CPU,WorkingSet64 | ConvertTo-Json -Compress',
  ].join('; ');
  const output = execFileSync('powershell.exe', ['-NoProfile', '-Command', script], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  }).trim();
  if (!output) return [];
  const parsed = JSON.parse(output);
  const items = Array.isArray(parsed) ? parsed : [parsed];
  return items.map((item) => ({
    pid: Number(item.Id),
    cpuSeconds: Number(item.CPU ?? 0),
    memoryBytes: Number(item.WorkingSet64 ?? 0),
  }));
}

function sampleProcesses(pids, procRoot = '/proc') {
  if (!pids?.length) return null;
  try {
    const processes = process.platform === 'win32'
      ? parseWindowsProcessSample(pids)
      : process.platform === 'linux'
        ? parseLinuxProcessSample(pids, procRoot)
        : parseUnixProcessSample(pids);
    const found = new Set(processes.map((item) => item.pid));
    const missingPids = pids.filter((pid) => !found.has(pid));
    return {
      capturedAt: new Date().toISOString(),
      processes,
      error: missingPids.length ? `未找到 PID：${missingPids.join(',')}` : undefined,
    };
  } catch (error) {
    return { capturedAt: new Date().toISOString(), processes: [], error: error.message };
  }
}

async function makeRequest(baseUrl, scenario, requestIndex, timeoutMs) {
  const startedAt = new Date().toISOString();
  const started = performance.now();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  let status = null;
  let error = null;
  let success = false;
  try {
    const response = await fetch(`${baseUrl}${scenario.path}`, {
      method: scenario.method,
      headers: scenario.headers,
      body: scenario.body === undefined ? undefined : JSON.stringify(scenario.body),
      signal: controller.signal,
    });
    status = response.status;
    const text = await response.text();
    success = status === scenario.expectedStatus;
    if (success && scenario.expectedJson) {
      try {
        const json = JSON.parse(text);
        success = getJsonPath(json, scenario.expectedJson.path) === scenario.expectedJson.equals;
        if (!success) error = `响应 ${scenario.expectedJson.path} 不等于 ${scenario.expectedJson.equals}`;
      } catch (parseError) {
        success = false;
        error = `响应不是合法 JSON：${parseError.message}`;
      }
    }
    if (!success && !error) error = `HTTP ${status}，期望 ${scenario.expectedStatus}`;
  } catch (requestError) {
    error = requestError.name === 'AbortError' ? `请求超过 ${timeoutMs}ms` : requestError.message;
  } finally {
    clearTimeout(timeout);
  }
  return {
    requestIndex,
    startedAt,
    scenario: scenario.name,
    method: scenario.method,
    path: scenario.path,
    status,
    success,
    durationMs: round(performance.now() - started),
    error,
  };
}

async function runRequests(baseUrl, scenarios, totalRequests, concurrency, timeoutMs) {
  const records = new Array(totalRequests);
  let cursor = 0;
  async function worker() {
    while (true) {
      const requestIndex = cursor;
      cursor += 1;
      if (requestIndex >= totalRequests) return;
      const scenario = scenarios[requestIndex % scenarios.length];
      records[requestIndex] = await makeRequest(baseUrl, scenario, requestIndex, timeoutMs);
    }
  }
  const workerCount = Math.min(concurrency, totalRequests);
  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return records;
}

function summarizeResources(samples, durationSeconds) {
  const validSamples = samples.filter((sample) => !sample?.error && sample?.processes?.length);
  if (validSamples.length === 0) {
    return { sampleCount: samples.length, available: false };
  }
  const memoryTotals = validSamples.map((sample) => sample.processes.reduce((sum, item) => sum + item.memoryBytes, 0));
  let cpuTotals = [];
  if (validSamples.some((sample) => sample.processes.some((item) => item.cpuSeconds !== undefined))) {
    cpuTotals = [];
    for (let index = 1; index < validSamples.length; index += 1) {
      const previous = validSamples[index - 1];
      const current = validSamples[index];
      const previousCpu = previous.processes.reduce((sum, item) => sum + (item.cpuSeconds ?? 0), 0);
      const currentCpu = current.processes.reduce((sum, item) => sum + (item.cpuSeconds ?? 0), 0);
      const elapsed = (new Date(current.capturedAt) - new Date(previous.capturedAt)) / 1000;
      if (elapsed > 0 && currentCpu >= previousCpu) cpuTotals.push(((currentCpu - previousCpu) / elapsed) * 100);
    }
  } else {
    cpuTotals = validSamples.map((sample) => sample.processes.reduce((sum, item) => sum + (item.cpuPercent ?? 0), 0));
  }
  return {
    available: true,
    sampleCount: validSamples.length,
    durationSeconds: round(durationSeconds),
    averageCpuPercent: cpuTotals.length ? round(cpuTotals.reduce((sum, value) => sum + value, 0) / cpuTotals.length) : null,
    peakCpuPercent: cpuTotals.length ? round(Math.max(...cpuTotals)) : null,
    averageMemoryMiB: round(memoryTotals.reduce((sum, value) => sum + value, 0) / memoryTotals.length / 1024 ** 2),
    peakMemoryMiB: round(Math.max(...memoryTotals) / 1024 ** 2),
  };
}

function summarizeRun(target, runNumber, records, elapsedMs, resourceSamples, options) {
  const durations = records.map((record) => record.durationMs).sort((a, b) => a - b);
  const succeeded = records.filter((record) => record.success).length;
  const failed = records.length - succeeded;
  const scenarioSummaries = Object.fromEntries([...new Set(records.map((record) => record.scenario))].map((name) => {
    const matching = records.filter((record) => record.scenario === name);
    const matchingDurations = matching.map((record) => record.durationMs).sort((a, b) => a - b);
    return [name, {
      requests: matching.length,
      throughputRps: round(matching.length / (elapsedMs / 1000)),
      averageMs: round(matchingDurations.reduce((sum, value) => sum + value, 0) / matchingDurations.length),
      p95Ms: round(percentile(matchingDurations, 0.95)),
      errors: matching.filter((record) => !record.success).length,
    }];
  }));
  return {
    target: target.label,
    baseUrl: target.baseUrl,
    run: runNumber,
    concurrency: options.concurrency,
    requests: records.length,
    succeeded,
    failed,
    errorRatePercent: round((failed / records.length) * 100),
    elapsedSeconds: round(elapsedMs / 1000),
    throughputRps: round(records.length / (elapsedMs / 1000)),
    averageMs: round(durations.reduce((sum, value) => sum + value, 0) / durations.length),
    p95Ms: round(percentile(durations, 0.95)),
    p99Ms: round(percentile(durations, 0.99)),
    minMs: round(durations[0]),
    maxMs: round(durations.at(-1)),
    resources: summarizeResources(resourceSamples, elapsedMs / 1000),
    scenarios: scenarioSummaries,
  };
}

function aggregateSummaries(summaries) {
  const labels = [...new Set(summaries.map((summary) => summary.target))];
  return labels.map((label) => {
    const runs = summaries.filter((summary) => summary.target === label);
    const average = (field) => round(runs.reduce((sum, item) => sum + item[field], 0) / runs.length);
    const resourceRuns = runs.filter((item) => item.resources.available);
    const resourceAverage = (field) => resourceRuns.length
      ? round(resourceRuns.reduce((sum, item) => sum + (item.resources[field] ?? 0), 0) / resourceRuns.length)
      : null;
    return {
      target: label,
      runCount: runs.length,
      concurrency: runs[0].concurrency,
      averageThroughputRps: average('throughputRps'),
      averageResponseMs: average('averageMs'),
      averageP95Ms: average('p95Ms'),
      averageErrorRatePercent: average('errorRatePercent'),
      averageCpuPercent: resourceAverage('averageCpuPercent'),
      averageMemoryMiB: resourceAverage('averageMemoryMiB'),
      peakMemoryMiB: resourceRuns.length ? Math.max(...resourceRuns.map((item) => item.resources.peakMemoryMiB)) : null,
    };
  });
}

function csvEscape(value) {
  const text = value === null || value === undefined ? '' : String(value);
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

function summariesCsv(summaries) {
  const fields = [
    'target', 'run', 'concurrency', 'requests', 'throughputRps', 'averageMs', 'p95Ms',
    'p99Ms', 'errorRatePercent', 'elapsedSeconds', 'averageCpuPercent', 'peakCpuPercent',
    'averageMemoryMiB', 'peakMemoryMiB',
  ];
  const lines = [fields.join(',')];
  for (const summary of summaries) {
    const row = {
      ...summary,
      averageCpuPercent: summary.resources.averageCpuPercent,
      peakCpuPercent: summary.resources.peakCpuPercent,
      averageMemoryMiB: summary.resources.averageMemoryMiB,
      peakMemoryMiB: summary.resources.peakMemoryMiB,
    };
    lines.push(fields.map((field) => csvEscape(row[field])).join(','));
  }
  return `${lines.join('\n')}\n`;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    console.log(usage());
    return;
  }
  const scenarioDocument = JSON.parse(await readFile(options.scenariosPath, 'utf8'));
  if (!Array.isArray(scenarioDocument.scenarios) || scenarioDocument.scenarios.length === 0) {
    throw new Error('场景文件必须包含非空 scenarios 数组');
  }

  const rawPath = path.join(options.outputPath, 'raw');
  const resourcePath = path.join(options.outputPath, 'resources');
  await mkdir(rawPath, { recursive: true });
  await mkdir(resourcePath, { recursive: true });
  await writeFile(path.join(options.outputPath, 'machine.json'), `${JSON.stringify(machineInfo(), null, 2)}\n`);
  await writeFile(path.join(options.outputPath, 'test-config.json'), `${JSON.stringify({
    capturedAt: new Date().toISOString(),
    dataset: scenarioDocument.dataset,
    scenariosPath: path.relative(process.cwd(), options.scenariosPath),
    targets: options.targets,
    runs: options.runs,
    concurrency: options.concurrency,
    requestsPerRun: options.requests,
    timeoutMs: options.timeoutMs,
    warmupRequests: options.warmupRequests,
    resourceIntervalMs: options.resourceIntervalMs,
    cooldownMs: options.cooldownMs,
    pids: Object.fromEntries(options.pids),
    procRoot: options.procRoot,
  }, null, 2)}\n`);

  const summaries = [];
  for (const target of options.targets) {
    console.log(`\n[${target.label}] 预热 ${options.warmupRequests} 个请求：${target.baseUrl}`);
    const warmup = await runRequests(
      target.baseUrl,
      scenarioDocument.scenarios,
      options.warmupRequests,
      Math.min(options.concurrency, 10),
      options.timeoutMs,
    );
    const warmupFailures = warmup.filter((record) => !record.success);
    if (warmupFailures.length > 0) {
      throw new Error(`[${target.label}] 预热失败 ${warmupFailures.length}/${warmup.length}，首个错误：${warmupFailures[0].error}`);
    }

    for (let runNumber = 1; runNumber <= options.runs; runNumber += 1) {
      if (runNumber > 1 && options.cooldownMs > 0) {
        console.log(`[${target.label}] 冷却 ${options.cooldownMs} ms 后开始下一轮`);
        await new Promise((resolve) => setTimeout(resolve, options.cooldownMs));
      }
      console.log(`[${target.label}] 第 ${runNumber}/${options.runs} 轮：并发 ${options.concurrency}，请求 ${options.requests}`);
      const resourceSamples = [];
      const pids = options.pids.get(target.label);
      if (pids?.length) {
        const initialSample = sampleProcesses(pids, options.procRoot);
        if (initialSample.error) throw new Error(`[${target.label}] 资源采样失败：${initialSample.error}`);
        resourceSamples.push(initialSample);
      }
      const sampler = pids?.length ? setInterval(() => resourceSamples.push(sampleProcesses(pids, options.procRoot)), options.resourceIntervalMs) : null;
      const started = performance.now();
      const records = await runRequests(
        target.baseUrl,
        scenarioDocument.scenarios,
        options.requests,
        options.concurrency,
        options.timeoutMs,
      );
      const elapsedMs = performance.now() - started;
      if (sampler) clearInterval(sampler);
      if (pids?.length) resourceSamples.push(sampleProcesses(pids, options.procRoot));
      const resourceFailure = resourceSamples.find((sample) => sample?.error);
      if (resourceFailure) throw new Error(`[${target.label}] 资源采样失败：${resourceFailure.error}`);
      const summary = summarizeRun(target, runNumber, records, elapsedMs, resourceSamples, options);
      summaries.push(summary);
      const runId = `${target.label}-run-${String(runNumber).padStart(2, '0')}`;
      await writeFile(path.join(rawPath, `${runId}.jsonl`), `${records.map((record) => JSON.stringify(record)).join('\n')}\n`);
      await writeFile(path.join(resourcePath, `${runId}.jsonl`), `${resourceSamples.map((sample) => JSON.stringify(sample)).join('\n')}\n`);
      console.log(`  吞吐 ${summary.throughputRps} req/s，平均 ${summary.averageMs} ms，P95 ${summary.p95Ms} ms，错误率 ${summary.errorRatePercent}%`);
    }
  }

  const aggregate = aggregateSummaries(summaries);
  await writeFile(path.join(options.outputPath, 'summary.json'), `${JSON.stringify({ summaries, aggregate }, null, 2)}\n`);
  await writeFile(path.join(options.outputPath, 'summary.csv'), summariesCsv(summaries));
  console.log(`\n结果已保存：${options.outputPath}`);
  console.table(aggregate);
}

main().catch((error) => {
  console.error(`性能测试失败：${error.message}`);
  process.exitCode = 1;
});
