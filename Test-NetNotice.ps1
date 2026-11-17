$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/netnotice') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d out @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
foreach ($suite in @('Member3IntegrationTest','FunctionalRegressionTest','DesktopWorkflowTest')) {
    & java -cp out "netnotice.test.$suite"
    if ($LASTEXITCODE -ne 0) { throw "$suite failed." }
}
Write-Host 'All three NetNotice test suites passed.'
