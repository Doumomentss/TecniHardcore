$ErrorActionPreference='Stop'
$agentPath=Join-Path $PSScriptRoot 'playit.exe'
$secretPath=Join-Path $PSScriptRoot 'playit-secret.json'
$socketPath='\\.\pipe\tecnihardcore-playitd'
if(-not (Test-Path -LiteralPath $agentPath -PathType Leaf)){throw 'Falta playit/playit.exe. Restaura el agente antes de iniciar el tunel.'}
if(-not (Test-Path -LiteralPath $secretPath -PathType Leaf)){throw 'Falta la vinculacion de Playit (playit-secret.json). No crees otro tunel: restaura su configuracion.'}
$agentMutex=New-Object System.Threading.Mutex($false,'Local\TecniHardcore-Playit')
$agentLock=$false
try {
  $agentLock=$agentMutex.WaitOne(10000)
  if(-not $agentLock){throw 'Otro proceso esta iniciando Playit. Espera unos segundos y vuelve a intentar.'}
  $agent=Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq 'playit.exe' -and $_.ExecutablePath -eq $agentPath -and
    $_.CommandLine -like ('*--secret-path*'+$secretPath+'*') -and
    $_.CommandLine -like ('*--socket-path*'+$socketPath+'*')
  } | Select-Object -First 1
  if($agent){Write-Host "Playit ya esta abierto (PID $($agent.ProcessId)); se reutiliza el agente vinculado.";return}
  $agentProcess=Start-Process -FilePath $agentPath -ArgumentList @(
    '--socket-path',$socketPath,'--secret-path',('"'+$secretPath+'"'),
    '--log-path',('"'+(Join-Path $PSScriptRoot 'tecni-daemon.log')+'"')
  ) -WindowStyle Hidden -PassThru
  Start-Sleep -Seconds 1
  if($agentProcess.HasExited){throw "Playit no pudo mantenerse abierto (codigo $($agentProcess.ExitCode)). Revisa playit/tecni-daemon.log localmente; no compartas el archivo secreto."}
  Write-Host "Playit iniciado en segundo plano (PID $($agentProcess.Id))."
} finally {
  if($agentLock){$agentMutex.ReleaseMutex()}
  $agentMutex.Dispose()
}
