import { createServer } from 'node:http';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';

const webOrigin = process.env.E2E_WEB_ORIGIN ?? 'http://127.0.0.1:8090';
const apiOrigin = process.env.E2E_API_ORIGIN ?? 'http://127.0.0.1:8080';
const artifactsRoot = process.env.E2E_ARTIFACTS_ROOT ?? join(process.cwd(), 'build');

const url = new URL(webOrigin);
const port = Number(url.port || 80);
const host = url.hostname;

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.wasm': 'application/wasm',
  '.otf': 'font/otf',
};

function resolveStatic(pathname) {
  const clean = decodeURIComponent(pathname).split('?')[0];
  const candidates = [
    join(artifactsRoot, clean),
    join(artifactsRoot, clean, 'index.html'),
  ];
  for (const candidate of candidates) {
    const resolved = normalize(candidate);
    if (!resolved.startsWith(artifactsRoot)) continue;
    if (existsSync(resolved) && statSync(resolved).isFile()) return resolved;
  }
  return null;
}

const server = createServer(async (req, res) => {
  const pathname = req.url?.split('?')[0] ?? '/';

  if (pathname.startsWith('/api/') || pathname.startsWith('/actuator/')) {
    const upstream = new URL(apiOrigin + (req.url ?? '/'));
    const headers = { ...req.headers };
    headers.host = upstream.host;
    const chunks = [];
    for await (const chunk of req) chunks.push(chunk);
    const body = chunks.length ? Buffer.concat(chunks) : undefined;
    const proxy = fetch(upstream, {
      method: req.method,
      headers,
      body,
      redirect: 'manual',
    });
    proxy
      .then(async (resp) => {
        res.writeHead(resp.status, Object.fromEntries(resp.headers.entries()));
        if (resp.body) {
          const reader = resp.body.getReader();
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            res.write(value);
          }
        }
        res.end();
      })
      .catch((error) => {
        res.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' });
        res.end(`代理后端失败: ${error.message}`);
      });
    return;
  }

  let file = resolveStatic(pathname);
  if (!file && pathname.startsWith('/web/')) {
    file = join(artifactsRoot, 'web', 'index.html');
  } else if (!file && pathname.startsWith('/merchant/')) {
    file = join(artifactsRoot, 'merchant', 'index.html');
  } else if (!file && pathname.startsWith('/admin/')) {
    file = join(artifactsRoot, 'admin', 'index.html');
  }
  if (!file || !existsSync(file)) {
    res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    res.end('Not found');
    return;
  }
  const type = mime[extname(file).toLowerCase()] ?? 'application/octet-stream';
  res.writeHead(200, { 'content-type': type });
  createReadStream(file).pipe(res);
});

server.listen(port, host, () => {
  console.log(`[e2e] static server listening on ${webOrigin}, artifacts=${artifactsRoot}, api=${apiOrigin}`);
});
