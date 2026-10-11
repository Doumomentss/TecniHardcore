$ErrorActionPreference='Stop'
$cli='D:\playlit\bin\playit.exe'
$config='C:\ProgramData\playit_gg\playit.toml'
if(-not (Test-Path -LiteralPath $cli -PathType Leaf)){throw 'Falta Playit 1.0.10 en D:\playlit. Reinstálalo antes de iniciar el servidor.'}
if(-not (Test-Path -LiteralPath $config -PathType Leaf)){throw 'Playit no está vinculado. Ejecuta playit setup antes de iniciar el servidor.'}
$status=& $cli status 2>&1 | Out-String
if($status -notmatch 'Secret configured:\s+true'){throw "Playit existe pero no tiene una cuenta vinculada.`n$status"}
if($status -match 'Phase:\s+(stopped|waiting for secret)'){
  & $cli start | Out-Null
  Start-Sleep -Seconds 1
  $status=& $cli status 2>&1 | Out-String
}
if($status -notmatch 'Phase:\s+(starting|running|connected)'){throw "Playit no pudo iniciar correctamente.`n$status"}
Write-Host 'Playit 1.0.10 activo; se reutiliza el agente vinculado.'
