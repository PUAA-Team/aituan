$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$MavenRepo = "D:/aituan_cache/m2"
$NpmCache = "D:/aituan_cache/npm"
$PubCache = "D:/aituan_cache/pub"

function Invoke-Native {
  param(
    [Parameter(Mandatory = $true)][string]$Title,
    [Parameter(Mandatory = $true)][string]$FilePath,
    [Parameter(Mandatory = $true)][string[]]$Arguments,
    [string]$WorkingDirectory = $RepoRoot
  )

  Write-Host ""
  Write-Host "== $Title =="
  Push-Location $WorkingDirectory
  try {
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
      throw "Command failed ($LASTEXITCODE): $FilePath $($Arguments -join ' ')"
    }
  }
  finally {
    Pop-Location
  }
}

$BackendUnitTests = @(
  "FileStorageServiceTest",
  "TradeGrowthServiceTest",
  "ComplaintServiceTest",
  "DiscoveryServiceTest",
  "StationMessagePublisherTest",
  "SupportServiceTest",
  "AiAgentServiceTest",
  "InteractionServiceTest"
) -join ","

Invoke-Native `
  -Title "Backend unit and service tests" `
  -FilePath "mvn" `
  -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/backend/pom.xml"), "-Dmaven.repo.local=$MavenRepo", "-Dtest=$BackendUnitTests", "test")

$PreviousPubCache = $env:PUB_CACHE
$env:PUB_CACHE = $PubCache
try {
  Invoke-Native `
    -Title "Flutter user app unit tests" `
    -FilePath "flutter.bat" `
    -Arguments @("test", "--coverage") `
    -WorkingDirectory (Join-Path $RepoRoot "apps/user_app")
}
finally {
  if ($null -eq $PreviousPubCache) {
    Remove-Item Env:\PUB_CACHE -ErrorAction SilentlyContinue
  }
  else {
    $env:PUB_CACHE = $PreviousPubCache
  }
}

Invoke-Native `
  -Title "Merchant web unit and API wrapper tests" `
  -FilePath "npm.cmd" `
  -Arguments @("--prefix", (Join-Path $RepoRoot "apps/merchant_web"), "--cache", $NpmCache, "run", "test:coverage")

Invoke-Native `
  -Title "Admin web unit and API wrapper tests" `
  -FilePath "npm.cmd" `
  -Arguments @("--prefix", (Join-Path $RepoRoot "apps/admin_web"), "--cache", $NpmCache, "run", "test:coverage")

Write-Host ""
Write-Host "Unit test classification script completed."
