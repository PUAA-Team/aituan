param(
  [switch]$IncludeE2E,
  [switch]$IncludeMysqlSmoke
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$UnitScript = Join-Path $RepoRoot "scripts/verify/test_unit.ps1"
$IntegrationScript = Join-Path $RepoRoot "scripts/verify/test_integration_api.ps1"
$E2eScript = Join-Path $RepoRoot "scripts/verify/test_e2e.ps1"

function Invoke-Script {
  param(
    [Parameter(Mandatory = $true)][string]$Title,
    [Parameter(Mandatory = $true)][string]$Path,
    [string[]]$Arguments = @()
  )

  if (-not (Test-Path $Path)) {
    throw "Script not found: $Path"
  }

  Write-Host ""
  Write-Host "== $Title =="
  & $Path @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Script failed ($LASTEXITCODE): $Path $($Arguments -join ' ')"
  }
}

Invoke-Script -Title "Classified unit tests" -Path $UnitScript

$IntegrationArgs = @()
if ($IncludeMysqlSmoke) {
  $IntegrationArgs += "-IncludeMysqlSmoke"
}
Invoke-Script -Title "Classified integration/API tests" -Path $IntegrationScript -Arguments $IntegrationArgs

if ($IncludeE2E) {
  Invoke-Script -Title "Classified E2E tests" -Path $E2eScript
}
else {
  Write-Host ""
  Write-Host "E2E skipped. Add -IncludeE2E for full acceptance tests."
}

Write-Host ""
Write-Host "All classified test scripts completed."
