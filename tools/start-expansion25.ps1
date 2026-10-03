$ErrorActionPreference='Stop'
$fixture=Join-Path $PSScriptRoot 'test-runtime\expansion25'
$server=Join-Path $fixture 'server'
if(-not (Test-Path -LiteralPath (Join-Path $server '.tecni-test-world'))) { throw 'Missing isolated-world marker.' }
$java=Get-ChildItem -LiteralPath (Join-Path $fixture 'game\runtime') -Filter java.exe -Recurse |
    Where-Object { $_.FullName -match 'jdk-17\.[^\\]+\\bin\\java.exe$' } | Select-Object -First 1
if(-not $java) { throw 'Prepare the isolated client first to obtain Java 17.' }
$version=(& $java.FullName -version 2>&1 | Out-String)
if($version -notmatch 'version "17\.') { throw 'Test server requires Java 17.' }
Push-Location $server
try { & $java.FullName '-Xms512M' '-Xmx4G' '-Dtecni.testServer=true' '-Dtecni.testRewards=true' '-jar' 'fabric-server-launch.jar' 'nogui' }
finally { Pop-Location }
