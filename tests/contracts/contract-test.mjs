#!/usr/bin/env node

import { createServer } from 'node:http';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

function usage() {
  return `跨服务消费者驱动契约测试

离线验证：
  node tests/contracts/contract-test.mjs

在线 Provider 验证：
  node tests/contracts/contract-test.mjs --live \\
    --provider identity-asset-service=http://127.0.0.1:18081 \\
    --provider merchant-catalog-service=http://127.0.0.1:18082

环境变量 CONTRACT_SERVICE_TOKEN 用于 X-Service-Token；默认值仅适合本地 demo。
`;
}

function parseLabelValue(value, option) {
  const separator = value.indexOf('=');
  if (separator <= 0 || separator === value.length - 1) {
    throw new Error(`${option} 格式应为 label=value，实际为 ${value}`);
  }
  return [value.slice(0, separator).trim(), value.slice(separator + 1).trim()];
}

function parseArgs(argv) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const options = {
    live: false,
    providers: new Map(),
    contractsPath: path.resolve('tests/contracts/internal-contracts.json'),
    outputPath: path.resolve(`tests/contracts/results/contract-report-${timestamp}.json`),
  };
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    if (option === '--help' || option === '-h') {
      options.help = true;
      continue;
    }
    if (option === '--live') {
      options.live = true;
      continue;
    }
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) throw new Error(`${option} 缺少参数值`);
    switch (option) {
      case '--provider': {
        const [provider, baseUrl] = parseLabelValue(value, option);
        options.providers.set(provider, new URL(baseUrl).toString().replace(/\/$/, ''));
        break;
      }
      case '--contracts':
        options.contractsPath = path.resolve(value);
        break;
      case '--output':
        options.outputPath = path.resolve(value);
        break;
      default:
        throw new Error(`未知参数：${option}`);
    }
    index += 1;
  }
  return options;
}

function getJsonPath(value, expression) {
  if (expression === '$') return value;
  if (!expression.startsWith('$.')) throw new Error(`不支持的 JSON 路径：${expression}`);
  return expression.slice(2).split('.').reduce((current, key) => current?.[key], value);
}

function actualType(value) {
  if (Array.isArray(value)) return 'array';
  if (value === null) return 'null';
  return typeof value;
}

function assertDocument(document, assertions, context) {
  for (const assertion of assertions ?? []) {
    const actual = getJsonPath(document, assertion.path);
    if (actualType(actual) !== assertion.type) {
      throw new Error(`${context} ${assertion.path} 类型为 ${actualType(actual)}，期望 ${assertion.type}`);
    }
    if (Object.hasOwn(assertion, 'equals') && actual !== assertion.equals) {
      throw new Error(`${context} ${assertion.path} 为 ${JSON.stringify(actual)}，期望 ${JSON.stringify(assertion.equals)}`);
    }
  }
}

function requestHeaders(contract, serviceToken) {
  return {
    'Content-Type': 'application/json',
    'X-Request-Id': `contract-${contract.id}`,
    'X-Caller-Service': contract.consumer,
    'X-Service-Token': serviceToken,
  };
}

async function readRequestBody(request) {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  if (chunks.length === 0) return undefined;
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function assertIncomingRequest(contract, request, body, serviceToken) {
  if (request.method !== contract.request.method) {
    throw new Error(`请求方法为 ${request.method}，期望 ${contract.request.method}`);
  }
  if (request.url !== contract.request.path) {
    throw new Error(`请求路径为 ${request.url}，期望 ${contract.request.path}`);
  }
  for (const name of contract.request.requiredHeaders ?? []) {
    if (!request.headers[name.toLowerCase()]) throw new Error(`缺少请求头 ${name}`);
  }
  for (const [name, expected] of Object.entries(contract.request.expectedHeaders ?? {})) {
    const actual = request.headers[name.toLowerCase()];
    if (name.toLowerCase() === 'content-type') {
      if (!actual?.startsWith(expected)) throw new Error(`请求头 ${name} 为 ${actual}，期望以 ${expected} 开头`);
    } else if (actual !== expected) {
      throw new Error(`请求头 ${name} 为 ${actual}，期望 ${expected}`);
    }
  }
  if (request.headers['x-service-token'] !== serviceToken) throw new Error('X-Service-Token 未按运行配置发送');
  assertDocument(body, contract.request.assertions, '请求体');
}

async function runAgainstBaseUrl(contract, baseUrl, serviceToken) {
  const response = await fetch(`${baseUrl}${contract.request.path}`, {
    method: contract.request.method,
    headers: requestHeaders(contract, serviceToken),
    body: contract.request.body === undefined ? undefined : JSON.stringify(contract.request.body),
  });
  const responseText = await response.text();
  let responseBody;
  try {
    responseBody = JSON.parse(responseText);
  } catch {
    throw new Error(`Provider 返回的不是 JSON：${responseText.slice(0, 160)}`);
  }
  if (response.status !== contract.response.status) {
    throw new Error(`Provider 返回 HTTP ${response.status}，期望 ${contract.response.status}`);
  }
  assertDocument(responseBody, contract.response.assertions, '响应体');
  return { status: response.status, assertions: contract.response.assertions.length };
}

async function runMockContract(contract, serviceToken) {
  let requestFailure;
  const server = createServer(async (request, response) => {
    try {
      const body = await readRequestBody(request);
      assertIncomingRequest(contract, request, body, serviceToken);
      response.writeHead(contract.response.status, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify(contract.response.fixture));
    } catch (error) {
      requestFailure = error;
      response.writeHead(500, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ error: error.message }));
    }
  });
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  try {
    const address = server.address();
    const result = await runAgainstBaseUrl(contract, `http://127.0.0.1:${address.port}`, serviceToken);
    if (requestFailure) throw requestFailure;
    return result;
  } finally {
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  }
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    console.log(usage());
    return;
  }
  const document = JSON.parse(await readFile(options.contractsPath, 'utf8'));
  if (!Array.isArray(document.contracts) || document.contracts.length === 0) {
    throw new Error('契约文件必须包含非空 contracts 数组');
  }
  const duplicateIds = document.contracts.map((contract) => contract.id).filter((id, index, all) => all.indexOf(id) !== index);
  if (duplicateIds.length) throw new Error(`契约 ID 重复：${duplicateIds.join(', ')}`);

  const serviceToken = process.env.CONTRACT_SERVICE_TOKEN || 'dev-internal-token';
  const results = [];
  for (const contract of document.contracts) {
    const started = Date.now();
    try {
      let result;
      if (options.live) {
        const baseUrl = options.providers.get(contract.provider)
          || process.env[`${contract.provider.toUpperCase().replaceAll('-', '_')}_URL`];
        if (!baseUrl) throw new Error(`在线模式缺少 ${contract.provider} 的 --provider 或环境变量`);
        result = await runAgainstBaseUrl(contract, baseUrl.replace(/\/$/, ''), serviceToken);
      } else {
        result = await runMockContract(contract, serviceToken);
      }
      results.push({
        id: contract.id,
        consumer: contract.consumer,
        provider: contract.provider,
        mode: options.live ? 'live-provider' : 'consumer-mock',
        success: true,
        durationMs: Date.now() - started,
        ...result,
      });
      console.log(`PASS ${contract.id}`);
    } catch (error) {
      results.push({
        id: contract.id,
        consumer: contract.consumer,
        provider: contract.provider,
        mode: options.live ? 'live-provider' : 'consumer-mock',
        success: false,
        durationMs: Date.now() - started,
        error: error.message,
      });
      console.error(`FAIL ${contract.id}: ${error.message}`);
    }
  }

  const report = {
    generatedAt: new Date().toISOString(),
    contractVersion: document.version,
    dataset: document.dataset,
    mode: options.live ? 'live-provider' : 'consumer-mock',
    passed: results.filter((result) => result.success).length,
    failed: results.filter((result) => !result.success).length,
    results,
  };
  await mkdir(path.dirname(options.outputPath), { recursive: true });
  await writeFile(options.outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`契约报告：${options.outputPath}`);
  if (report.failed > 0) process.exitCode = 1;
}

main().catch((error) => {
  console.error(`契约测试失败：${error.message}`);
  process.exitCode = 1;
});
