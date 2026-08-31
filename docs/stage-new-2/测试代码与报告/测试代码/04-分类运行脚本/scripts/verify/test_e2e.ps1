$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$E2eScript = Join-Path $RepoRoot "tests/e2e/scripts/run-e2e-local.ps1"

if (-not (Test-Path $E2eScript)) {
  throw "E2E script not found: $E2eScript"
}

$PreviousPubCache = $env:PUB_CACHE
$PreviousNpmCache = $env:npm_config_cache
$env:PUB_CACHE = "D:/aituan_cache/pub"
$env:npm_config_cache = "D:/aituan_cache/npm"

try {
  Write-Host "== Playwright E2E tests (UC01-UC13) =="
  & $E2eScript
  if ($LASTEXITCODE -ne 0) {
    throw "E2E script failed with exit code: $LASTEXITCODE"
  }
}
finally {
  if ($null -eq $PreviousPubCache) {
    Remove-Item Env:\PUB_CACHE -ErrorAction SilentlyContinue
  }
  else {
    $env:PUB_CACHE = $PreviousPubCache
  }

  if ($null -eq $PreviousNpmCache) {
    Remove-Item Env:\npm_config_cache -ErrorAction SilentlyContinue
  }
  else {
    $env:npm_config_cache = $PreviousNpmCache
  }
}

Write-Host ""
Write-Host "E2E test classification script completed."
