param([switch]$Build)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$envPath = Join-Path $PSScriptRoot '.env'
if (Test-Path -LiteralPath $envPath) {
    foreach ($line in Get-Content -LiteralPath $envPath -Encoding UTF8) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') {
            $key = $matches[1]
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            if ($value) { [Environment]::SetEnvironmentVariable($key, $value, 'Process') }
        }
    }
}
New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'target') | Out-Null
if ($Build) {
    & mvn.cmd -q verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & java "-Djdk.net.unixdomain.tmpdir=$PSScriptRoot\target" '-Dfile.encoding=UTF-8' -jar (Join-Path $PSScriptRoot 'target\ai-chat-server-1.0.0.jar')
} else {
    & mvn.cmd spring-boot:run
}
exit $LASTEXITCODE
