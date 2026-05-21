param(
  [switch]$IncludeCache
)

$ErrorActionPreference = 'Stop'

$BuildDirs = @(
  'D:\aituan_build\user_app',
  'D:\aituan_build\user_app_preview',
  'D:\aituan_build\backend'
)

$RuntimeDirs = @(
  'D:\aituan_runtime\backend'
)

foreach ($Dir in $BuildDirs) {
  if (Test-Path $Dir) {
    Remove-Item $Dir -Recurse -Force
    Write-Host "Cleaned: $Dir"
  }
}

foreach ($Dir in $RuntimeDirs) {
  if (Test-Path $Dir) {
    Remove-Item $Dir -Recurse -Force
    Write-Host "Cleaned: $Dir"
  }
}

if ($IncludeCache) {
  $CacheDirs = @('D:\aituan_cache\pub', 'D:\aituan_cache\gradle')
  foreach ($Dir in $CacheDirs) {
    if (Test-Path $Dir) {
      Remove-Item $Dir -Recurse -Force
      Write-Host "Cleaned cache: $Dir"
    }
  }
}

Write-Host 'Clean complete. APK files stay in D:\aituan_release\apk\ by default.'
