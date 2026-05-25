param(
  [string]$ServerOrigin = 'http://182.92.238.178',
  [switch]$SkipApk
)

$ErrorActionPreference = 'Stop'

function Invoke-ServerScript {
  param(
    [string]$Name,
    [string]$Path,
    [scriptblock]$Command
  )

  Write-Host "==== $Name ===="
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Name failed with exit code $LASTEXITCODE"
  }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendScript = Join-Path $ScriptDir 'build_backend_server.ps1'
$FrontendsScript = Join-Path $ScriptDir 'build_frontends_server.ps1'
$ApkScript = Join-Path $ScriptDir 'build_android_apk_server.ps1'

Invoke-ServerScript 'build-backend-server' $BackendScript { & $BackendScript }
Invoke-ServerScript 'build-frontends-server' $FrontendsScript { & $FrontendsScript -ServerOrigin $ServerOrigin }

if (-not $SkipApk) {
  Invoke-ServerScript 'build-user-apk-server' $ApkScript { & $ApkScript -ServerOrigin $ServerOrigin }
}

Write-Host "Server artifacts are ready for origin: $ServerOrigin"
