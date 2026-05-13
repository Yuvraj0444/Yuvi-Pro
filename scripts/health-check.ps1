#Requires -Version 5.1
<#
.SYNOPSIS
    Checks the health of all SkyWays microservices.
.DESCRIPTION
    For each service:
      - Calls /actuator/health and reports the status (UP / DOWN / UNREACHABLE)
      - Queries the Eureka REST API to verify the service is registered
    Prints a formatted summary table and exits with code 1 if any service is unhealthy.
.EXAMPLE
    .\scripts\health-check.ps1
.EXAMPLE
    # In a CI pipeline — non-zero exit code means a service is down:
    .\scripts\health-check.ps1; if ($LASTEXITCODE -ne 0) { throw "Health check failed" }
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# ── Service catalog ───────────────────────────────────────────────────────────
# EurekaName   = spring.application.name in UPPER_CASE as Eureka stores it.
#                Empty string means the service does not register (Eureka itself).
$SERVICES = @(
    @{ Name = "Eureka Registry";       Port = 8761; EurekaName = ""                              },
    @{ Name = "Config Server";         Port = 8888; EurekaName = "SKYWAYS-CONFIG-SERVER"         },
    @{ Name = "API Gateway";           Port = 8080; EurekaName = "SKYWAYS-GATEWAY"               },
    @{ Name = "User Service";          Port = 8081; EurekaName = "SKYWAYS-USER-SERVICE"          },
    @{ Name = "Flight Service";        Port = 8082; EurekaName = "SKYWAYS-FLIGHT-SERVICE"        },
    @{ Name = "Booking Service";       Port = 8083; EurekaName = "SKYWAYS-BOOKING-SERVICE"       },
    @{ Name = "Payment Service";       Port = 8084; EurekaName = "SKYWAYS-PAYMENT-SERVICE"       },
    @{ Name = "Notification Service";  Port = 8085; EurekaName = "SKYWAYS-NOTIFICATION-SERVICE"  },
    @{ Name = "Saga Orchestrator";     Port = 8086; EurekaName = "SKYWAYS-SAGA-ORCHESTRATOR"     }
)

$EUREKA_APPS_URL   = "http://localhost:8761/eureka/apps"
$ACTUATOR_TIMEOUT  = 5    # seconds per health request
$COL_NAME          = 26   # column width for service name
$COL_STATUS        = 14   # column width for health status

# ── Fetch the set of app names currently registered in Eureka ─────────────────
function Get-EurekaRegistrations {
    try {
        $response = Invoke-RestMethod `
            -Uri     $EUREKA_APPS_URL `
            -Headers @{ Accept = "application/json" } `
            -TimeoutSec 5 `
            -ErrorAction Stop

        $registered = @{}

        # applications.application may be $null, an object, or an array depending on
        # how many apps are registered. Wrap in @() to always get an array.
        if ($null -ne $response.applications -and
            $null -ne $response.applications.application) {

            foreach ($app in @($response.applications.application)) {
                if ($null -ne $app.name) {
                    $registered[$app.name.ToUpper()] = $true
                }
            }
        }
        return $registered
    } catch {
        return $null   # Eureka unreachable
    }
}

# ── Call /actuator/health and return the status string ───────────────────────
function Get-ActuatorHealth {
    param([int]$Port)
    try {
        $resp = Invoke-RestMethod `
            -Uri        "http://localhost:$Port/actuator/health" `
            -Method     Get `
            -TimeoutSec $ACTUATOR_TIMEOUT `
            -ErrorAction Stop
        if ([string]::IsNullOrEmpty($resp.status)) { return "UNKNOWN" }
        return $resp.status.ToUpper()
    } catch {
        return "UNREACHABLE"
    }
}

# ── Render one row of the results table ──────────────────────────────────────
function Write-ResultRow {
    param(
        [string]$ServiceName,
        [string]$HealthStatus,
        [string]$EurekaStatus    # "REGISTERED" | "NOT REGISTERED" | "N/A" | "EUREKA DOWN"
    )

    $namePad   = $ServiceName.PadRight($COL_NAME)
    $statusPad = $HealthStatus.PadRight($COL_STATUS)

    # Health colour
    if ($HealthStatus -eq "UP") {
        $healthColor = "Green"
    } elseif ($HealthStatus -eq "UNREACHABLE") {
        $healthColor = "Red"
    } else {
        $healthColor = "Yellow"
    }

    # Eureka colour
    if ($EurekaStatus -eq "REGISTERED") {
        $eurekaColor = "Green"
    } elseif ($EurekaStatus -eq "N/A") {
        $eurekaColor = "DarkGray"
    } elseif ($EurekaStatus -eq "EUREKA DOWN") {
        $eurekaColor = "DarkYellow"
    } else {
        $eurekaColor = "Red"
    }

    Write-Host "  $namePad" -NoNewline
    Write-Host $statusPad   -NoNewline -ForegroundColor $healthColor
    Write-Host $EurekaStatus            -ForegroundColor $eurekaColor
}

# ── Banner and column headers ─────────────────────────────────────────────────
Write-Host ""
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host "   SkyWays Airlines  -  Health Check" -ForegroundColor Magenta
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host ""
Write-Host ("  " + "Service".PadRight($COL_NAME) + "Health".PadRight($COL_STATUS) + "Eureka") -ForegroundColor DarkGray
Write-Host ("  " + ("-" * ($COL_NAME + $COL_STATUS + 18))) -ForegroundColor DarkGray

# ── Fetch Eureka registrations once up front ──────────────────────────────────
Write-Host "  Querying Eureka registrations..." -ForegroundColor DarkGray
$registrations = Get-EurekaRegistrations

if ($null -eq $registrations) {
    Write-Host "  (Eureka unreachable - registration column will show EUREKA DOWN)" -ForegroundColor DarkYellow
}

Write-Host ""

# ── Check each service ────────────────────────────────────────────────────────
$upCount   = 0
$downCount = 0

foreach ($svc in $SERVICES) {
    $health = Get-ActuatorHealth -Port $svc.Port

    # Determine Eureka registration column value
    if ([string]::IsNullOrEmpty($svc.EurekaName)) {
        # Eureka server does not register with itself
        $eurekaStatus = "N/A"
    } elseif ($null -eq $registrations) {
        $eurekaStatus = "EUREKA DOWN"
    } elseif ($registrations.ContainsKey($svc.EurekaName)) {
        $eurekaStatus = "REGISTERED"
    } else {
        $eurekaStatus = "NOT REGISTERED"
    }

    Write-ResultRow -ServiceName $svc.Name -HealthStatus $health -EurekaStatus $eurekaStatus

    if ($health -eq "UP") { $upCount++ } else { $downCount++ }
}

# ── Summary footer ────────────────────────────────────────────────────────────
Write-Host ""
Write-Host ("  " + ("-" * ($COL_NAME + $COL_STATUS + 18))) -ForegroundColor DarkGray

if ($downCount -eq 0) {
    Write-Host "  All $upCount/$($SERVICES.Count) services are UP." -ForegroundColor Green
} else {
    $upColor = if ($upCount -gt 0) { "Yellow" } else { "Red" }
    Write-Host "  $upCount UP  |  $downCount DOWN or UNREACHABLE" -ForegroundColor $upColor
}

Write-Host ""
Write-Host "  Eureka dashboard : http://localhost:8761" -ForegroundColor DarkCyan
Write-Host "  Swagger UI       : http://localhost:8080/swagger-ui.html" -ForegroundColor DarkCyan
Write-Host ""

# Exit with a non-zero code so CI pipelines can detect failures
if ($downCount -gt 0) { exit 1 }