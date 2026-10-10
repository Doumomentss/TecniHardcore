param([switch]$BuildOnly)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
  $version=(Get-Content launcher/package.json -Raw | ConvertFrom-Json).version
  if(-not $BuildOnly){
    gh release view "v$version" --repo Doumomentss/TecniHardcore --json tagName 2>$null | Out-Null
    if($LASTEXITCODE -eq 0){throw "La versión $version ya está publicada. Incrementa package.json y package-lock.json antes de publicar otra."}
  }
  node --test launcher/test/updater.test.js
  if($LASTEXITCODE -ne 0){throw 'Updater tests failed'}
  python tools/test-installer-transactions.py
  if($LASTEXITCODE -ne 0){throw 'Installer transaction tests failed'}
  & (Join-Path $PSScriptRoot 'package-launcher.ps1')
  python tools/build-installer.py
  if($LASTEXITCODE -ne 0){throw 'Installer build failed'}
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
  Set-Content -LiteralPath $notes -Encoding utf8 -Value "TecniHardcore $version inaugura una temporada nueva: Overworld, Nether y End generados de cero, plaza original en 0,96,0 y cinco vidas para todos. El progreso de juego se reinicia; cuentas, contraseñas, UUID y permisos se conservan. Cada jugador ve una cinemática individual de 30 segundos después de autenticarse por primera vez en el mundo nuevo. Incluye el botón ENTRAR AL SERVIDOR y actualización automática del launcher. Consulta NOVEDADES-2.9.md y RESULTADOS-2.9.md. Cierra Minecraft antes de instalar la actualización."
  gh release create "v$version" @assets --repo Doumomentss/TecniHardcore --draft --target main --title "TecniHardcore $version" --notes-file $notes
  if($LASTEXITCODE -ne 0){throw 'Draft release upload failed. No release was published.'}
  gh release view "v$version" --repo Doumomentss/TecniHardcore --json assets,isDraft --jq '.assets[] | {name,size}'
  if($LASTEXITCODE -ne 0){throw 'Draft release verification failed'}
  node tools/verify-release.cjs
  if($LASTEXITCODE -ne 0){throw "Los hashes publicados no coinciden. La release permanece en borrador."}
  gh release edit "v$version" --repo Doumomentss/TecniHardcore --draft=false --latest
  if($LASTEXITCODE -ne 0){throw 'Release publication failed'}
} finally {Pop-Location}
