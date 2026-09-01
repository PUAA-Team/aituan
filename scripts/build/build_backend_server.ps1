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

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir '..\..')
$BackendDir = Join-Path $RepoRoot 'services\backend'
$BuildDir = 'D:\aituan_build\backend_server'
$ArtifactDir = Join-Path $RepoRoot 'deploy\artifacts\backend'
$MavenRepo = 'D:\aituan_cache\m2'
$JavaHome = 'C:\Program Files\Java\jdk-21'
$JarName = 'aituan-backend.jar'

New-Item -ItemType Directory -Force -Path $BuildDir, $ArtifactDir, $MavenRepo | Out-Null
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

$MavenRepoArg = "-Dmaven.repo.local=$MavenRepo"
$BuildDirArg = "-Dbackend.build.directory=$BuildDir\target"

Invoke-Step 'package-backend-server' {
  mvn -f "$BackendDir\pom.xml" $MavenRepoArg '-DskipTests' $BuildDirArg clean package
}

$BuiltJar = Join-Path $BuildDir 'target\aituan-backend-0.0.1-SNAPSHOT.jar'
$TargetJar = Join-Path $ArtifactDir $JarName
Copy-Item $BuiltJar $TargetJar -Force
Remove-DirectoryIfExists $BuildDir

Write-Host "Backend server jar output: $TargetJar"
