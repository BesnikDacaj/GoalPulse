$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "backend\src"
$out = Join-Path $root "backend\out"

if (!(Test-Path $out)) {
  New-Item -ItemType Directory -Path $out | Out-Null
}

javac -encoding UTF-8 -d $out (Get-ChildItem -Path $src -Filter *.java | ForEach-Object { $_.FullName })
java -cp $out GoalPulseServer
