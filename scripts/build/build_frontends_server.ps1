param(
  [string]$ServerOrigin = 'http://182.92.238.178'
)

$ErrorActionPreference = 'Stop'

function Invoke-Step {
  param(
    [string]$Name,
    [scriptblock]$Command
  )

  Write-Host "==== $Name ===="
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Name failed with exit code $LASTEXITCODE"
  }
}

function Normalize-Origin {
  param([string]$Value)

  $Trimmed = $Value.Trim()
  while ($Trimmed.EndsWith('/')) {
    $Trimmed = $Trimmed.Substring(0, $Trimmed.Length - 1)
  }
  return $Trimmed
}

function Install-NpmDependencies {
  param([string]$AppDir)

  $LockFile = Join-Path $AppDir 'package-lock.json'
  if (Test-Path $LockFile) {
    npm ci --prefix $AppDir
  } else {
    npm install --prefix $AppDir --package-lock=false
  }
}

function Build-WebApp {
  param(
    [string]$Name,
    [string]$AppDir,
    [string]$BasePath,
    [string]$OutDir
  )

  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutDir) | Out-Null
  Invoke-Step "$Name-install" { Install-NpmDependencies $AppDir }
  Invoke-Step "$Name-build-server" {
    npm run build --prefix $AppDir -- --base=$BasePath --outDir $OutDir --emptyOutDir
  }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$MerchantDir = Join-Path $RepoRoot 'apps\merchant_web'
$AdminDir = Join-Path $RepoRoot 'apps\admin_web'
$MerchantOut = Join-Path $RepoRoot 'deploy\artifacts\merchant-web'
$AdminOut = Join-Path $RepoRoot 'deploy\artifacts\admin-web'
$NpmCache = 'D:\aituan_cache\npm'
$Origin = Normalize-Origin $ServerOrigin

New-Item -ItemType Directory -Force -Path $NpmCache | Out-Null
$PreviousNpmCache = $env:npm_config_cache
$PreviousApiBase = $env:VITE_API_BASE_URL

try {
  $env:npm_config_cache = $NpmCache
  $env:VITE_API_BASE_URL = $Origin
  Build-WebApp 'merchant-web' $MerchantDir '/merchant/' $MerchantOut
  Build-WebApp 'admin-web' $AdminDir '/admin/' $AdminOut
} finally {
  if ($null -eq $PreviousNpmCache) {
    Remove-Item Env:npm_config_cache -ErrorAction SilentlyContinue
  } else {
    $env:npm_config_cache = $PreviousNpmCache
  }

  if ($null -eq $PreviousApiBase) {
    Remove-Item Env:VITE_API_BASE_URL -ErrorAction SilentlyContinue
  } else {
    $env:VITE_API_BASE_URL = $PreviousApiBase
  }
}

Write-Host "Merchant web server output: $MerchantOut"
Write-Host "Admin web server output: $AdminOut"
Write-Host "Server API origin: $Origin"
