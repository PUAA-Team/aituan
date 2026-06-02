$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$SourceDir = Join-Path $RepoRoot 'apps\user_app'
$WorkDir = 'D:\aituan_build\user_app_preview'
$PubCache = 'D:\aituan_cache\pub'
$GradleHome = 'D:\aituan_cache\gradle'

New-Item -ItemType Directory -Force -Path $WorkDir, $PubCache, $GradleHome | Out-Null
$env:PUB_CACHE = $PubCache
$env:GRADLE_USER_HOME = $GradleHome

robocopy $SourceDir $WorkDir /MIR /XD .dart_tool build .gradle /XF *.apk | Out-Host
if ($LASTEXITCODE -gt 7) { exit $LASTEXITCODE }
$global:LASTEXITCODE = 0

Set-Location $WorkDir
flutter pub get
flutter run
