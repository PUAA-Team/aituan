param(
  [switch]$IncludeMysqlSmoke
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$MavenRepo = "D:/aituan_cache/m2"

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

$BackendApiTests = "*ApiIntegrationTest,*ApiTest,*ControllerTest,*ContractTest"

Invoke-Native `
  -Title "Backend integration and API tests" `
  -FilePath "mvn" `
  -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/backend/pom.xml"), "-Dmaven.repo.local=$MavenRepo", "-Dtest=$BackendApiTests", "test")

if ($IncludeMysqlSmoke) {
  $PreviousMysqlEnabled = $env:AITUAN_MYSQL_CI_ENABLED
  $env:AITUAN_MYSQL_CI_ENABLED = "true"
  try {
    Invoke-Native `
      -Title "MySQL migration smoke test" `
      -FilePath "mvn" `
      -Arguments @("-B", "-f", (Join-Path $RepoRoot "services/backend/pom.xml"), "-Dmaven.repo.local=$MavenRepo", "-Dtest=MysqlMigrationSmokeTest", "test")
  }
  finally {
    if ($null -eq $PreviousMysqlEnabled) {
      Remove-Item Env:\AITUAN_MYSQL_CI_ENABLED -ErrorAction SilentlyContinue
    }
    else {
      $env:AITUAN_MYSQL_CI_ENABLED = $PreviousMysqlEnabled
    }
  }
}
else {
  Write-Host ""
  Write-Host "MySQL smoke skipped. Add -IncludeMysqlSmoke to run it."
}

Write-Host ""
Write-Host "Integration/API test classification script completed."
