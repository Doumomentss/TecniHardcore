param([string]$OutputPath = '')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) { $OutputPath = Join-Path $root 'TecniHardcore Launcher/resources/app.asar' }
$outputFull = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($outputFull)) | Out-Null
$stage = Join-Path $root 'tools/build-launcher-stage'
$rootFull = [IO.Path]::GetFullPath($root).TrimEnd('\') + '\'
$stageFull = [IO.Path]::GetFullPath($stage)
if (-not $stageFull.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase)) { throw "Invalid stage path: $stageFull" }
if (Test-Path -LiteralPath $stageFull) { Remove-Item -LiteralPath $stageFull -Recurse -Force }
New-Item -ItemType Directory -Path $stageFull | Out-Null
Get-ChildItem -LiteralPath (Join-Path $root 'launcher') -File | Where-Object { $_.Extension -in '.js','.json','.html','.css' } | Copy-Item -Destination $stageFull
Copy-Item -LiteralPath (Join-Path $root 'launcher/assets') -Destination $stageFull -Recurse
Copy-Item -LiteralPath (Join-Path $root 'installer_payload') -Destination (Join-Path $stageFull 'pack') -Recurse
$catalog = Get-Content -LiteralPath (Join-Path $stageFull 'pack/mods-downloads.json') -Raw | ConvertFrom-Json
foreach ($mod in $catalog.files) {
  if ($mod.name -notmatch '^[A-Za-z0-9_.+() -]+\.jar$') { throw 'Invalid mod filename' }
  Remove-Item -LiteralPath (Join-Path $stageFull ('pack/mods/' + $mod.name)) -Force
}
Push-Location $stageFull
try {
  & npm.cmd ci --omit=dev --ignore-scripts --no-audit --no-fund
  if ($LASTEXITCODE -ne 0) { throw 'Failed to install launcher runtime dependencies' }
} finally { Pop-Location }
Push-Location $root
try {
  node -e "require('./launcher/node_modules/asar').createPackage('tools/build-launcher-stage',process.argv[1]).then(()=>console.log('packaged production dependencies and official mod catalog'))" $outputFull
  if ($LASTEXITCODE -ne 0) { throw 'Failed to create app.asar' }
} finally { Pop-Location }
