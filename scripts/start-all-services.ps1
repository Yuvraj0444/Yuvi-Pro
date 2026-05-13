#Requires -Version 5.1
<#
.SYNOPSIS
    Starts all SkyWays microservices in the correct startup order.
.DESCRIPTION
    1. Loads environment variables from scripts/secrets.env via load-secrets.ps1
    2. Verifies Java is available
    3. Checks each service port for conflicts
    4. Opens each service in a dedicated, titled PowerShell window
    5. Applies startup delays so infrastructure is ready before dependents start
.EXAMPLE
    # From the project root:
    .\scripts\start-all-services.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# ── Paths ─────────────────────────────────────────────────────────────────────
# $PSScriptRoot = D:\Skyways\skyways-airlines\scripts
# $ROOT         = D:\Skyways\skyways-airlines
$ROOT    = Split-Path -Parent $PSScriptRoot
$SCRIPTS = $PSScriptRoot

# ── Service catalog ───────────────────────────────────────────────────────────
# Delay = seconds to wait AFTER launching before opening the next service window.
# Eureka must be up before Config Server registers; Config must be up before
# application services fetch their configuration.
$SERVICES = @(
    @{ Title = "SkyWays :: Eureka Registry";        Dir = "skyways-registry";             Port = 8761; Delay = 20 },
    @{ Title = "SkyWays :: Config Server";           Dir = "skyways-config-server";        Port = 8888; Delay = 15 },
    @{ Title = "SkyWays :: API Gateway";             Dir = "skyways-gateway";              Port = 8080; Delay = 8  },
    @{ Title = "SkyWays :: User Service";            Dir = "skyways-user-service";         Port = 8081; Delay = 5  },
    @{ Title = "SkyWays :: Flight Service";          Dir = "skyways-flight-service";       Port = 8082; Delay = 5  },
    @{ Title = "SkyWays :: Booking Service";         Dir = "skyways-booking-service";      Port = 8083; Delay = 5  },
    @{ Title = "SkyWays :: Payment Service";         Dir = "skyways-payment-service";      Port = 8084; Delay = 5  },
    @{ Title = "SkyWays :: Notification Service";    Dir = "skyways-notification-service"; Port = 8085; Delay = 5  },
    @{ Title = "SkyWays :: Saga Orchestrator";       Dir = "skyways-saga-orchestrator";    Port = 8086; Delay = 0  }
)

# ── Output helpers ────────────────────────────────────────────────────────────
function Write-Step {
    param([string]$Message)
    Write-Host "  >> $Message" -ForegroundColor Cyan
}
function Write-OK {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}
function Write-Warn {
    param([string]$Message)
    Write-Host "  [!!] $Message" -ForegroundColor Yellow
}
function Write-Fail {
    param([string]$Message)
    Write-Host "  [XX] $Message" -ForegroundColor Red
}

# ── Test whether a TCP port is already listening ───────────────────────────────
function Test-PortInUse {
    param([int]$Port)
    $result = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return ($null -ne $result)
}

# ── Find the executable Spring Boot JAR for a service module ──────────────────
function Get-ServiceJar {
    param([string]$ServiceDir)
    $targetPath = Join-Path $ROOT "$ServiceDir\target"
    if (-not (Test-Path $targetPath)) { return $null }

    # Exclude Maven-produced side artifacts: *.original, *-sources.jar, *-javadoc.jar
    $jar = Get-ChildItem -Path $targetPath -Filter "*.jar" -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch 'original|sources|javadoc' } |
           Sort-Object LastWriteTime -Descending |
           Select-Object -First 1

    if ($null -eq $jar) { return $null }
    return $jar.FullName
}

# ── Open a service in a new titled PowerShell window ─────────────────────────
function Start-Microservice {
    param(
        [string]$Title,
        [string]$JarPath,
        [int]   $Port,
        [int]   $Delay
    )

    Write-Step "$Title  (port $Port)"

    # Guard: JAR must exist (i.e. 'mvn clean install' must have been run)
    if ([string]::IsNullOrEmpty($JarPath) -or -not (Test-Path $JarPath)) {
        Write-Fail "JAR not found for $Title."
        Write-Fail "Run from project root:  mvn clean install"
        return $false
    }

    # Guard: skip if something already occupies the port
    if (Test-PortInUse -Port $Port) {
        Write-Warn "Port $Port already in use. Skipping $Title (may already be running)."
        return $true
    }

    # Build the script that will run inside the new window.
    # -EncodedCommand is used to avoid all shell-quoting issues with paths and titles.
    # Variables ($Title, $Port, $JarPath) are expanded NOW by the here-string;
    # $host is escaped so it is evaluated inside the child window.
    $windowScript = @"
`$host.UI.RawUI.WindowTitle = '$Title'
Write-Host ''
Write-Host '  $Title' -ForegroundColor Cyan
Write-Host "  Port : $Port"   -ForegroundColor DarkGray
Write-Host "  JAR  : $JarPath" -ForegroundColor DarkGray
Write-Host ''
java -jar '$JarPath'
"@

    $encoded = [Convert]::ToBase64String(
        [System.Text.Encoding]::Unicode.GetBytes($windowScript)
    )

    Start-Process powershell.exe -ArgumentList "-NoExit", "-NoLogo", "-EncodedCommand", $encoded
    Write-OK "Window opened for $Title"

    if ($Delay -gt 0) {
        Write-Host "       Waiting ${Delay}s for $Title to initialize..." -ForegroundColor DarkGray
        Start-Sleep -Seconds $Delay
    }

    return $true
}

# ── Banner ────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host "   SkyWays Airlines  -  Start All Services" -ForegroundColor Magenta
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host ""

# ── Step 1: Load environment secrets ─────────────────────────────────────────
$secretsScript = Join-Path $SCRIPTS "load-secrets.ps1"
if (-not (Test-Path $secretsScript)) {
    Write-Fail "load-secrets.ps1 not found at: $secretsScript"
    exit 1
}
Write-Step "Loading environment variables from secrets.env..."
. $secretsScript   # dot-source: variables stay in this session and are inherited by child processes

# ── Step 2: Verify Java ───────────────────────────────────────────────────────
try {
    $javaOutput = (& java -version 2>&1)[0].ToString()
    Write-OK "Java: $javaOutput"
} catch {
    Write-Fail "Java not found on PATH. Install JDK 17+ and add it to PATH."
    exit 1
}

Write-Host ""
Write-Host "  Starting 9 services. This window can be closed once all windows open." -ForegroundColor DarkGray
Write-Host ""

# ── Step 3: Launch services in order ─────────────────────────────────────────
$launched = 0
$failed   = 0

foreach ($svc in $SERVICES) {
    $jar = Get-ServiceJar -ServiceDir $svc.Dir
    $ok  = Start-Microservice -Title $svc.Title -JarPath $jar -Port $svc.Port -Delay $svc.Delay
    if ($ok) { $launched++ } else { $failed++ }
}

# ── Step 4: Summary ───────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  ---------------------------------------------" -ForegroundColor DarkGray
if ($failed -eq 0) {
    Write-Host "  All $launched service windows opened successfully." -ForegroundColor Green
} else {
    Write-Host "  $launched launched, $failed failed. Check errors above." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "  Allow ~60s for all services to fully start, then:" -ForegroundColor DarkGray
Write-Host ""
Write-Host "    Eureka dashboard  : http://localhost:8761" -ForegroundColor DarkCyan
Write-Host "    Swagger UI        : http://localhost:8080/swagger-ui.html" -ForegroundColor DarkCyan
Write-Host "    Health check      : .\scripts\health-check.ps1" -ForegroundColor DarkCyan
Write-Host "    Stop all          : .\scripts\stop-all-services.ps1" -ForegroundColor DarkCyan
Write-Host ""