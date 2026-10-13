param([switch]$BuildOnly,[string]$LauncherBuildPath='',[string]$AppAsarPath='')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
  $version=(Get-Content launcher/package.json -Raw | ConvertFrom-Json).version
  if(-not $BuildOnly){
    $previousErrorPreference=$ErrorActionPreference
    $ErrorActionPreference='SilentlyContinue'
    gh release view "v$version" --repo Doumomentss/TecniHardcore --json tagName 2>$null | Out-Null
    $releaseExists=($LASTEXITCODE -eq 0)
    $ErrorActionPreference=$previousErrorPreference
    if($releaseExists){throw "La versión $version ya está publicada. Incrementa package.json y package-lock.json antes de publicar otra."}
  }
  node --test launcher/test/updater.test.js
  if($LASTEXITCODE -ne 0){throw 'Updater tests failed'}
  python tools/test-installer-transactions.py
  if($LASTEXITCODE -ne 0){throw 'Installer transaction tests failed'}
  if($AppAsarPath){& (Join-Path $PSScriptRoot 'package-launcher.ps1') -OutputPath $AppAsarPath}
  else{& (Join-Path $PSScriptRoot 'package-launcher.ps1')}
  $previousLauncherBuild=$env:TECNI_LAUNCHER_BUILD
  try{
    if($LauncherBuildPath){$env:TECNI_LAUNCHER_BUILD=[IO.Path]::GetFullPath($LauncherBuildPath)}
    python tools/build-installer.py
    if($LASTEXITCODE -ne 0){throw 'Installer build failed'}
  }finally{$env:TECNI_LAUNCHER_BUILD=$previousLauncherBuild}
  python tools/create-release-manifest.py
  if($LASTEXITCODE -ne 0){throw 'Manifest generation failed'}
  python tools/export-public-repo.py
  if($LASTEXITCODE -ne 0){throw 'Public export failed'}
  if($BuildOnly){return}
  $version=(Get-Content launcher/package.json -Raw | ConvertFrom-Json).version
  $repo=Join-Path $root 'publish/TecniHardcore'
  if(-not (Test-Path -LiteralPath (Join-Path $repo '.git'))){throw 'Initialize the curated public repository first.'}
  git -C $repo add --all
  git -C $repo diff --cached --quiet
  if($LASTEXITCODE -eq 1){git -C $repo commit -m "Release $version";if($LASTEXITCODE -ne 0){throw 'Git commit failed'}}
  git -C $repo push origin main
  if($LASTEXITCODE -ne 0){throw 'Git push failed'}
  $pack=(Get-Content installer_payload/config/tecnihardcore/connection.json -Raw | ConvertFrom-Json).packVersion
  $assets=@("dist/TecniHardcore-Setup-$version.exe",'dist/update.json',"dist/SHA256SUMS-$version.txt","dist/manifest-$version.json",'dist/THIRD-PARTY-MODS.json',"installer_payload/mods/tecnihardcore-$pack.jar",'installer_payload/mods/tecni-death-overlay-1.0.0.jar')
  $notes=Join-Path $root 'dist/release-notes.md'
  Set-Content -LiteralPath $notes -Encoding utf8 -Value "Corrige la actualización del launcher: verifica que el instalador se abra antes de cerrar la ventana y muestra errores de instalación. El servidor acepta la huella oficial del paquete 2.9.3. Cierra Minecraft antes de actualizar."
  gh release create "v$version" @assets --repo Doumomentss/TecniHardcore --draft --target main --title "TecniHardcore $version" --notes-file $notes
  if($LASTEXITCODE -ne 0){throw 'Draft release upload failed. No release was published.'}
  gh release view "v$version" --repo Doumomentss/TecniHardcore --json assets,isDraft --jq '.assets[] | {name,size}'
  if($LASTEXITCODE -ne 0){throw 'Draft release verification failed'}
  node tools/verify-release.cjs
  if($LASTEXITCODE -ne 0){throw "Los hashes publicados no coinciden. La release permanece en borrador."}
  gh release edit "v$version" --repo Doumomentss/TecniHardcore --draft=false --latest
  if($LASTEXITCODE -ne 0){throw 'Release publication failed'}
} finally {Pop-Location}
