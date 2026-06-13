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
  $global:LASTEXITCODE = 0
  & $Command
  if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) {
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

function Get-GitCommit {
  param([string]$Root)

  try {
    $Commit = git -C $Root rev-parse HEAD 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($Commit)) {
      return $Commit.Trim()
    }
  } catch {}
  return 'local'
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

function Build-FlutterUserWeb {
  param(
    [string]$AppDir,
    [string]$BaseHref,
    [string]$OutDir,
    [string]$Origin
  )

  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutDir) | Out-Null
  Push-Location $AppDir
  try {
    Invoke-Step 'user-web-pub-get' { flutter pub get }
    Invoke-Step 'user-web-build-server' {
      if (Test-Path $OutDir) {
        Remove-Item $OutDir -Recurse -Force
      }
      flutter build web --base-href $BaseHref "--dart-define=API_BASE_URL=$Origin" "--dart-define=AITUAN_BUILD_COMMIT=$BuildCommit" "--dart-define=AITUAN_BUILD_SOURCE=script" --output $OutDir
    }
  } finally {
    Pop-Location
  }
}

function Sync-LandingPage {
  param(
    [string]$SourceDir,
    [string]$OutDir
  )

  Invoke-Step 'landing-sync-server' {
    if (-not (Test-Path $SourceDir)) {
      throw "Landing source not found: $SourceDir"
    }
    if (Test-Path $OutDir) {
      Remove-Item $OutDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    Copy-Item -Path (Join-Path $SourceDir '*') -Destination $OutDir -Recurse -Force
  }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$MerchantDir = Join-Path $RepoRoot 'apps\merchant_web'
$AdminDir = Join-Path $RepoRoot 'apps\admin_web'
$UserAppDir = Join-Path $RepoRoot 'apps\user_app'
$LandingDir = Join-Path $RepoRoot 'deploy\landing'
$MerchantOut = Join-Path $RepoRoot 'deploy\artifacts\merchant-web'
$AdminOut = Join-Path $RepoRoot 'deploy\artifacts\admin-web'
$UserWebOut = Join-Path $RepoRoot 'deploy\artifacts\user-web'
$LandingOut = Join-Path $RepoRoot 'deploy\artifacts\landing'
$NpmCache = 'D:\aituan_cache\npm'
$PubCache = 'D:\aituan_cache\pub'
$Origin = Normalize-Origin $ServerOrigin
$BuildCommit = Get-GitCommit $RepoRoot

New-Item -ItemType Directory -Force -Path $NpmCache | Out-Null
New-Item -ItemType Directory -Force -Path $PubCache | Out-Null
$PreviousNpmCache = $env:npm_config_cache
$PreviousApiBase = $env:VITE_API_BASE_URL
$PreviousPubCache = $env:PUB_CACHE

try {
  $env:npm_config_cache = $NpmCache
  $env:VITE_API_BASE_URL = $Origin
  $env:PUB_CACHE = $PubCache
  Sync-LandingPage $LandingDir $LandingOut
  Build-FlutterUserWeb $UserAppDir '/web/' $UserWebOut $Origin
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

  if ($null -eq $PreviousPubCache) {
    Remove-Item Env:PUB_CACHE -ErrorAction SilentlyContinue
  } else {
    $env:PUB_CACHE = $PreviousPubCache
  }
}

Write-Host "Landing page server output: $LandingOut"
Write-Host "User web server output: $UserWebOut"
Write-Host "Merchant web server output: $MerchantOut"
Write-Host "Admin web server output: $AdminOut"
Write-Host "Server API origin: $Origin"
