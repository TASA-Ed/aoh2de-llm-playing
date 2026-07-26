$ErrorActionPreference = 'Stop'

# Gradle emits UTF-8 diagnostics; make Windows PowerShell decode and render them correctly.
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
chcp 65001 | Out-Null

& "$PSScriptRoot\gradlew.bat" @args
exit $LASTEXITCODE
