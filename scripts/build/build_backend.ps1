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
$BackendDir = Join-Path $RepoRoot 'services\backend'
$BuildDir = 'D:\aituan_build\backend'
$ReleaseDir = 'D:\aituan_release\backend'
$MavenRepo = 'D:\aituan_cache\m2'
$JavaHome = 'C:\Program Files\Java\jdk-21'
$JarName = 'aituan-backend.jar'

New-Item -ItemType Directory -Force -Path $BuildDir, $ReleaseDir, $MavenRepo | Out-Null
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

$MavenRepoArg = "-Dmaven.repo.local=$MavenRepo"
$BuildDirArg = "-Dbackend.build.directory=$BuildDir\target"

Invoke-Step 'package-backend' {
  mvn -f "$BackendDir\pom.xml" $MavenRepoArg '-DskipTests' $BuildDirArg clean package
}

$BuiltJar = Join-Path $BuildDir 'target\aituan-backend-0.0.1-SNAPSHOT.jar'
$TargetJar = Join-Path $ReleaseDir $JarName
Copy-Item $BuiltJar $TargetJar -Force
Remove-Item $BuildDir -Recurse -Force

Write-Host "Backend jar output: $TargetJar"
