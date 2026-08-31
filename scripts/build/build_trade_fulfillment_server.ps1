$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

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

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..\..')).Path
$ArtifactDir = Join-Path $RepoRoot 'deploy\artifacts\trade-fulfillment-service'
$MavenRepo = 'D:/aituan_cache/m2'
$JavaHome = 'D:/tools/jdk-17.0.18+8'
$JarName = 'aituan-trade-fulfillment-service.jar'
$BuiltJar = Join-Path $RepoRoot 'services\trade-fulfillment-service\target\trade-fulfillment-service-0.0.1-SNAPSHOT.jar'
$TargetJar = Join-Path $ArtifactDir $JarName

New-Item -ItemType Directory -Force -Path $ArtifactDir, $MavenRepo | Out-Null
if (Test-Path $JavaHome) {
  $env:JAVA_HOME = $JavaHome
  $env:Path = "$JavaHome\bin;$env:Path"
}

Invoke-Step 'package-trade-fulfillment-service' {
  mvn -B -f (Join-Path $RepoRoot 'services\pom.xml') '-pl' 'common-contract,trade-fulfillment-service' '-am' "-Dmaven.repo.local=$MavenRepo" '-DskipTests' clean package
}

if (-not (Test-Path $BuiltJar)) {
  throw "Built jar not found: $BuiltJar"
}

Copy-Item $BuiltJar $TargetJar -Force
Write-Host "Trade fulfillment server jar output: $TargetJar"
