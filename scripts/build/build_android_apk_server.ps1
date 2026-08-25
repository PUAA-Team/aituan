param(
  [string]$ServerOrigin = 'http://182.92.238.178'
)

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

function Remove-DirectoryIfExists {
  param([string]$Path)

  if (-not (Test-Path $Path)) { return }
  $CmdPath = $Path.Replace('/', '\')
  cmd.exe /c "if exist `"$CmdPath`" rmdir /s /q `"$CmdPath`""
  if (Test-Path $Path) {
    throw "Failed to remove work dir: $Path"
  }
}

function Normalize-Origin {
  param([string]$Value)

  $Trimmed = $Value.Trim()
  while ($Trimmed.EndsWith('/')) {
    $Trimmed = $Trimmed.Substring(0, $Trimmed.Length - 1)
  }
  return $Trimmed
}

function Get-GitCommit {
  param([string]$Root)

  try {
    $Commit = git -C $Root rev-parse HEAD 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($Commit)) {
      return $Commit.Trim()
    }
  } catch {}
  return 'local'
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$SourceDir = Join-Path $RepoRoot 'apps\user_app'
$WorkDir = 'D:\aituan_build\user_app_server'
$ReleaseDir = 'D:\aituan_release\apk'
$ArtifactDir = Join-Path $RepoRoot 'deploy\artifacts\downloads'
$PubCache = 'D:\aituan_cache\pub'
$GradleHome = 'D:\aituan_cache\gradle'
$AppVersion = (Select-String -Path (Join-Path $SourceDir 'pubspec.yaml') -Pattern '^version:\s*(.+)$').Matches.Groups[1].Value.Trim()
$SafeAppVersion = $AppVersion.Replace('+', '-')
$ApkName = "aituan-user-$SafeAppVersion-server-debug.apk"
$Origin = Normalize-Origin $ServerOrigin
$BuildCommit = Get-GitCommit $RepoRoot

New-Item -ItemType Directory -Force -Path $WorkDir, $ReleaseDir, $ArtifactDir, $PubCache, $GradleHome | Out-Null
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
  Invoke-Step 'build-apk-debug-server' {
    flutter build apk --debug "--dart-define=API_BASE_URL=$Origin" "--dart-define=AITUAN_BUILD_COMMIT=$BuildCommit" "--dart-define=AITUAN_BUILD_SOURCE=script"
  }
} finally {
  Pop-Location
}

$BuiltApk = Join-Path $WorkDir 'build\app\outputs\flutter-apk\app-debug.apk'
$TargetApk = Join-Path $ReleaseDir $ApkName
$ArtifactApk = Join-Path $ArtifactDir $ApkName
Copy-Item $BuiltApk $TargetApk -Force
Copy-Item $BuiltApk $ArtifactApk -Force
Remove-DirectoryIfExists $WorkDir

Write-Host "Server API origin: $Origin"
Write-Host "App version: $AppVersion"
Write-Host "Build commit: $BuildCommit"
Write-Host "APK server output: $TargetApk"
Write-Host "APK deploy artifact: $ArtifactApk"
