# Loads scripts/secrets.env into the current PowerShell session.
# DOT-SOURCE this script so the variables persist in your shell:
#
#   . .\scripts\load-secrets.ps1
#
# Running without the leading dot runs in a child process and
# the variables disappear when it exits.

$envFile = Join-Path $PSScriptRoot "secrets.env"

if (-not (Test-Path $envFile)) {
    Write-Error "secrets.env not found at: $envFile"
    exit 1
}

$loaded  = 0
$skipped = 0

foreach ($line in Get-Content $envFile) {
    $line = $line.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { continue }

    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { continue }

    $key   = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()

    if ($value -like "REPLACE_WITH*") {
        Write-Warning "  [$key] still has a placeholder - update secrets.env"
        $skipped++
        continue
    }

    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    $loaded++
}

# Build display strings as variables to avoid nested-quote parsing issues
# in Windows PowerShell 5.1.
$maskedPass = ""
if ($env:DB_PASS -ne $null -and $env:DB_PASS.Length -gt 0) {
    $maskedPass = ("*" * $env:DB_PASS.Length)
}
$eurekaUrl = "http://" + $env:EUREKA_HOST + ":8761"

Write-Host ""
Write-Host "Secrets loaded: $loaded set, $skipped skipped (placeholders)." -ForegroundColor Green
Write-Host "  DB_HOST = $env:DB_HOST"
Write-Host "  DB_USER = $env:DB_USER"
Write-Host "  DB_PASS = $maskedPass (masked)"
Write-Host "  EUREKA  = $eurekaUrl"
Write-Host ""