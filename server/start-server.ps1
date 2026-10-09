param([ValidateSet(6,8)][int]$MaxMemoryGB=6)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$workspace=Split-Path -Parent $PSScriptRoot
$portLine=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'server.properties') | Where-Object {$_ -match '^server-port='} | Select-Object -First 1
$serverPort=if($portLine){[int]($portLine -split '=',2)[1]}else{25565}
& (Join-Path $workspace 'playit/start-playit.ps1')
$listener=Get-NetTCPConnection -State Listen -LocalPort $serverPort -ErrorAction SilentlyContinue
if($listener){Write-Host "Ya hay un proceso escuchando en el puerto $serverPort (PID $($listener[0].OwningProcess)). No se abrirá otro servidor.";exit 0}
$mutex=New-Object System.Threading.Mutex($false,('Local\TecniHardcore-Server-'+$serverPort))
if(-not $mutex.WaitOne(0)){Write-Host "Ya se está iniciando el servidor en el puerto $serverPort.";$mutex.Dispose();exit 0}
try {
$runtimeDirectory=Join-Path $workspace 'client/runtime'
if(-not (Test-Path -LiteralPath $runtimeDirectory -PathType Container)){throw 'Abre el juego una vez desde el launcher para instalar Java 17.'}
if(-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'fabric-server-launch.jar') -PathType Leaf)){throw 'Falta server/fabric-server-launch.jar. Restaura los archivos del servidor.'}
$javaCandidates=Get-ChildItem -LiteralPath $runtimeDirectory -Directory | ForEach-Object {Join-Path $_.FullName 'bin/java.exe'}
$javaPath=$javaCandidates | Where-Object {Test-Path -LiteralPath $_} | Select-Object -First 1
$runtimeConfig=Join-Path $PSScriptRoot 'runtime-java.json'
$requiredJava=17
if(Test-Path -LiteralPath $runtimeConfig){
  $runtimeSettings=Get-Content -LiteralPath $runtimeConfig -Raw | ConvertFrom-Json
  if($runtimeSettings.major -ne 21){throw 'Configuración de Java del servidor inválida.'}
  $serverRuntimeRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'runtime')).TrimEnd('\')+'\'
  $verifiedJava=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot $runtimeSettings.java))
  if(-not $verifiedJava.StartsWith($serverRuntimeRoot,[StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $verifiedJava -PathType Leaf)){throw 'Falta el Java 21 del servidor. Ejecuta tools/prepare-moderation28.py.'}
  $javaPath=$verifiedJava;$requiredJava=21
}
if(-not $javaPath){throw 'Abre el juego una vez desde el launcher para instalar Java 17.'}
$javaCheck=New-Object System.Diagnostics.Process
$javaCheck.StartInfo=New-Object System.Diagnostics.ProcessStartInfo
$javaCheck.StartInfo.FileName=$javaPath
$javaCheck.StartInfo.Arguments='-version'
$javaCheck.StartInfo.UseShellExecute=$false
$javaCheck.StartInfo.CreateNoWindow=$true
$javaCheck.StartInfo.RedirectStandardError=$true
[void]$javaCheck.Start()
$versionText=$javaCheck.StandardError.ReadToEnd()
$javaCheck.WaitForExit()
$javaCheck.Dispose()
if($versionText -notmatch ('version "'+$requiredJava+'\.')){throw "El servidor requiere el runtime verificado de Java $requiredJava."}
if($MaxMemoryGB -eq 8){
  $memory=Get-CimInstance Win32_OperatingSystem
  if($memory.TotalVisibleMemorySize -lt 24GB/1KB -or $memory.FreePhysicalMemory -lt 12GB/1KB){throw 'No hay margen suficiente para asignar 8 GB con el cliente abierto. Usa 6 GB.'}
}
while($true){
  # Reuse or reopen our linked agent before restarting Minecraft after a crash.
  & (Join-Path $workspace 'playit/start-playit.ps1')
  # Java writes normal diagnostics to stderr; PowerShell 5 must not abort on it.
  $ErrorActionPreference='Continue'
  try { & $javaPath '-Xms2G' "-Xmx${MaxMemoryGB}G" '-Dtecni.allowTrialBoss=true' '-XX:+UseG1GC' '-XX:+ParallelRefProcEnabled' '-XX:MaxGCPauseMillis=200' '-jar' 'fabric-server-launch.jar' 'nogui';$result=$LASTEXITCODE }
  finally {$ErrorActionPreference='Stop'}
  if($result -eq 0){Write-Host 'Apagado solicitado. No se reiniciará.';break}
  Write-Warning "Fallo del servidor (código $result). Reinicio en 10 segundos."
  Start-Sleep -Seconds 10
}
} finally {$mutex.ReleaseMutex();$mutex.Dispose()}
