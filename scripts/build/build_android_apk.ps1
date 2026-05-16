$ErrorActionPreference = 'Stop'

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
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$SourceDir = Join-Path $RepoRoot 'apps\user_app'
$WorkDir = 'D:\aituan_build\user_app'
$ReleaseDir = 'D:\aituan_release\apk'
$PubCache = 'D:\aituan_cache\pub'
$GradleHome = 'D:\aituan_cache\gradle'
$ApkName = 'aituan-user-debug.apk'

New-Item -ItemType Directory -Force -Path $WorkDir, $ReleaseDir, $PubCache, $GradleHome | Out-Null
$env:PUB_CACHE = $PubCache
$env:GRADLE_USER_HOME = $GradleHome

robocopy $SourceDir $WorkDir /MIR /XD .dart_tool build .gradle /XF *.apk | Out-Host
if ($LASTEXITCODE -gt 7) { exit $LASTEXITCODE }
$global:LASTEXITCODE = 0

Push-Location $WorkDir
try {
  Invoke-Step 'pub-get' { flutter pub get }
  Invoke-Step 'analyze' { flutter analyze }
  Invoke-Step 'test' { flutter test }
  Invoke-Step 'build-apk-debug' { flutter build apk --debug }
} finally {
  Pop-Location
}

$BuiltApk = Join-Path $WorkDir 'build\app\outputs\flutter-apk\app-debug.apk'
$TargetApk = Join-Path $ReleaseDir $ApkName
Copy-Item $BuiltApk $TargetApk -Force
Remove-Item $WorkDir -Recurse -Force

Write-Host "APK output: $TargetApk"
