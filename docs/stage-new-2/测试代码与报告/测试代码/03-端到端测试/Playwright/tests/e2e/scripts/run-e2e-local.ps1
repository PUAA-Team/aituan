$ErrorActionPreference = "Stop"

try {
  $Utf8NoBom = New-Object System.Text.UTF8Encoding $false
  [Console]::OutputEncoding = $Utf8NoBom
  $OutputEncoding = $Utf8NoBom
} catch {}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\")
$E2eRoot = Join-Path $RepoRoot "tests\e2e"
$LocalRoot = $env:AITUAN_E2E_ROOT
if (-not $LocalRoot) { $LocalRoot = "D:\aituan_runtime\e2e" }
$BuildRoot = Join-Path $LocalRoot "web"
$BackendDir = Join-Path $LocalRoot "backend"
$JarPath = Join-Path $BackendDir "aituan-backend-e2e.jar"
$MavenRepo = $env:AITUAN_M2_REPO
if (-not $MavenRepo) { $MavenRepo = "D:\aituan_cache\m2" }
$JavaHome = $env:AITUAN_JAVA_HOME
if (-not $JavaHome) { $JavaHome = "D:\tools\jdk-17.0.18+8" }
if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
  throw "JDK 17 not found. Set AITUAN_JAVA_HOME. Current: $JavaHome"
}
$Maven = $env:AITUAN_MAVEN
if (-not $Maven) { $Maven = "D:\tools\apache-maven-3.9.14\bin\mvn.cmd" }
if (-not (Test-Path $Maven)) {
  throw "mvn.cmd not found. Set AITUAN_MAVEN. Current: $Maven"
}

$ApiOrigin = "http://127.0.0.1:8080"
$WebOrigin = "http://127.0.0.1:8090"
$EdgePath = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

New-Item -ItemType Directory -Force -Path $BuildRoot, $BackendDir | Out-Null

function Invoke-Native {
  param([Parameter(Mandatory = $true)][string]$FilePath, [string[]]$Arguments, [string]$WorkingDirectory)
  Push-Location $WorkingDirectory
  try {
    $quoted = $Arguments | ForEach-Object {
      if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }
    $process = Start-Process -FilePath $FilePath -ArgumentList $quoted -WorkingDirectory $WorkingDirectory -NoNewWindow -Wait -PassThru
    if ($process.ExitCode -ne 0) {
      throw "Command failed ($($process.ExitCode)): $FilePath $($Arguments -join ' ')"
    }
  }
  finally {
    Pop-Location
  }
}

Write-Host "[e2e] Building backend JAR..."
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Invoke-Native $Maven @("-B", "-Dmaven.repo.local=$MavenRepo", "-Dbackend.build.directory=$BackendDir", "-DskipTests", "package", "-f", (Join-Path $RepoRoot "services\backend\pom.xml")) $RepoRoot
$BuiltJar = Get-ChildItem -LiteralPath $BackendDir -Filter "aituan-backend-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $BuiltJar) { throw "Backend JAR build failed" }
Copy-Item -LiteralPath $BuiltJar.FullName -Destination $JarPath -Force

Write-Host "[e2e] Building Flutter user web..."
$env:PUB_CACHE = $env:PUB_CACHE
Invoke-Native "flutter.bat" @("build", "web", "--base-href", "/web/", "--dart-define=API_BASE_URL=$WebOrigin", "--dart-define=AITUAN_BUILD_COMMIT=local-e2e", "--dart-define=AITUAN_BUILD_SOURCE=e2e-local", "--output", (Join-Path $BuildRoot "web")) (Join-Path $RepoRoot "apps\user_app")

Write-Host "[e2e] Building merchant/admin web..."
foreach ($app in @("merchant_web", "admin_web")) {
  Push-Location (Join-Path $RepoRoot "apps\$app")
  try {
    if (-not (Test-Path "node_modules")) {
      Invoke-Native "npm.cmd" @("ci") (Join-Path $RepoRoot "apps\$app")
    }
    $env:VITE_API_BASE_URL = $WebOrigin
    Invoke-Native "npm.cmd" @("run", "build", "--", "--base=/$(($app -replace '_web',''))/", "--outDir", (Join-Path $BuildRoot ($app -replace '_web','')), "--emptyOutDir") (Join-Path $RepoRoot "apps\$app")
    Remove-Item Env:\VITE_API_BASE_URL -ErrorAction SilentlyContinue
  }
  finally {
    Pop-Location
  }
}

$env:E2E_API_ORIGIN = $ApiOrigin
$env:E2E_WEB_ORIGIN = $WebOrigin
$env:E2E_ARTIFACTS_ROOT = $BuildRoot
$env:PLAYWRIGHT_BROWSER_PATH = $EdgePath

Write-Host "[e2e] Starting backend..."
$backend = Start-Process -FilePath (Join-Path $JavaHome "bin\java.exe") -ArgumentList @(
  "-jar", $JarPath,
  "--spring.profiles.active=e2e",
  "--server.port=8080"
) -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $LocalRoot "backend.out.log") -RedirectStandardError (Join-Path $LocalRoot "backend.err.log")

$server = $null
try {
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    try {
      $resp = Invoke-RestMethod -Uri "$ApiOrigin/api/open/auth/token/check" -TimeoutSec 2
      if ($null -ne $resp.code) { $ready = $true; break }
    }
    catch { Start-Sleep -Seconds 2 }
  }
  if (-not $ready) {
    throw "Backend was not ready within 120 seconds. Check log: $($LocalRoot)\backend.err.log"
  }
  Write-Host "[e2e] Backend is ready. Starting static server at $WebOrigin ..."
  Push-Location $E2eRoot
  $server = Start-Process -FilePath "node.exe" -ArgumentList @("scripts/static-server.mjs") -PassThru -WindowStyle Hidden -WorkingDirectory $E2eRoot -RedirectStandardOutput (Join-Path $LocalRoot "server.out.log") -RedirectStandardError (Join-Path $LocalRoot "server.err.log")
  Start-Sleep -Seconds 2

  Write-Host "[e2e] Running Playwright E2E..."
  if (-not (Test-Path (Join-Path $E2eRoot "node_modules"))) {
    Invoke-Native "npm.cmd" @("ci") $E2eRoot
  }
  Invoke-Native "npx.cmd" @("playwright", "test") $E2eRoot
  Write-Host "[e2e] All E2E tests passed. Report: $E2eRoot\playwright-report"
}
finally {
  if ($server -and -not $server.HasExited) { Stop-Process -Id $server.Id -Force }
  if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force }
}
