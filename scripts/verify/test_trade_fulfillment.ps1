param(
  [switch]$IncludeMysqlSmoke
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$MavenRepo = "D:/aituan_cache/m2"
$JavaHome = "D:/tools/jdk-17.0.18+8"

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

New-Item -ItemType Directory -Force -Path $MavenRepo | Out-Null
if (Test-Path $JavaHome) {
  $env:JAVA_HOME = $JavaHome
  $env:Path = "$JavaHome\bin;$env:Path"
}

Invoke-Native `
  -Title "Trade fulfillment H2 tests" `
  -FilePath "mvn" `
  -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/pom.xml"), "-pl", "common-contract,trade-fulfillment-service", "-am", "-Dmaven.repo.local=$MavenRepo", "test")

Invoke-Native `
  -Title "Gateway trade route test" `
  -FilePath "mvn" `
  -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/pom.xml"), "-pl", "api-gateway", "-Dmaven.repo.local=$MavenRepo", "-Dtest=GatewayTradeRouteTest", "test")

if ($IncludeMysqlSmoke) {
  $PreviousMysqlEnabled = $env:AITUAN_TRADE_MYSQL_CI_ENABLED
  $env:AITUAN_TRADE_MYSQL_CI_ENABLED = "true"
  try {
    Invoke-Native `
      -Title "Trade fulfillment MySQL migration smoke test" `
      -FilePath "mvn" `
      -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/pom.xml"), "-pl", "common-contract,trade-fulfillment-service", "-am", "-Dmaven.repo.local=$MavenRepo", "-Dtest=TradeFulfillmentMysqlMigrationSmokeTest", "test")
  }
  finally {
    if ($null -eq $PreviousMysqlEnabled) {
      Remove-Item Env:\AITUAN_TRADE_MYSQL_CI_ENABLED -ErrorAction SilentlyContinue
    }
    else {
      $env:AITUAN_TRADE_MYSQL_CI_ENABLED = $PreviousMysqlEnabled
    }
  }
}
else {
  Write-Host ""
  Write-Host "Trade MySQL smoke skipped. Add -IncludeMysqlSmoke and set TRADE_DB_* to run it."
}

Write-Host ""
Write-Host "Trade fulfillment test script completed."
