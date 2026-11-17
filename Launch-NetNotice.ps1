param([ValidateSet('server','client')][string]$Mode = 'server')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/netnotice') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d out @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. See the errors above.' }
Write-Host "Opening NetNotice - $Mode workspace"
if ($Mode -eq 'server') { & java -cp out netnotice.server.NetNoticeServer } else { & java -cp out netnotice.client.NetNoticeClient }

