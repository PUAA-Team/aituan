$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$BuildScript = Join-Path $RepoRoot 'scripts\build\build_backend.ps1'
$ReleaseDir = 'D:\aituan_release\backend'
$BuildDir = 'D:\aituan_build\backend'
$RuntimeDir = 'D:\aituan_runtime\backend'
$MavenRepo = 'D:\aituan_cache\m2'
$JavaHome = 'C:\Program Files\Java\jdk-21'
$JarPath = Join-Path $ReleaseDir 'aituan-backend.jar'
$LogPath = Join-Path $RuntimeDir 'backend.log'

New-Item -ItemType Directory -Force -Path $BuildDir, $ReleaseDir, $RuntimeDir, $MavenRepo | Out-Null
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = 'demo'
if (-not (Test-Path $JarPath)) {
  Write-Host 'Backend jar not found, building first.'
  & $BuildScript
}

Write-Host "Starting backend from $JarPath"
java -jar $JarPath *>&1 | Tee-Object -FilePath $LogPath
