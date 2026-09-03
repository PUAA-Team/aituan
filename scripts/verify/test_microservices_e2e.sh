#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
e2e_dir="${repo_root}/tests/e2e"

export E2E_API_ORIGIN="${E2E_API_ORIGIN:-http://127.0.0.1:18080}"
export E2E_WEB_ORIGIN="${E2E_WEB_ORIGIN:-http://127.0.0.1:8090}"

if [[ ! -x "${e2e_dir}/node_modules/.bin/playwright" ]]; then
  echo "[e2e] Installing locked Playwright dependencies"
  npm ci --prefix "${e2e_dir}"
fi

if [[ "${AITUAN_E2E_INSTALL_BROWSER:-false}" == "true" ]]; then
  (cd "${e2e_dir}" && npx playwright install --with-deps chromium)
fi

echo "[e2e] Running UC01-UC13 through ${E2E_WEB_ORIGIN} and Gateway ${E2E_API_ORIGIN}"
cd "${e2e_dir}"
npx playwright test "$@"
