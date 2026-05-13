#Requires -Version 5.1
<#
.SYNOPSIS
    Stops all running SkyWays microservices.
.DESCRIPTION
    For each known service port, finds the process that owns the TCP listen
    socket and terminates it. Services are stopped in reverse startup order
    (dependents first, infrastructure last) so Eureka receives deregistration
    events where possible.
.EXAMPLE
    .\scripts\stop-all-services.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# ── Service catalog (reverse startup order) ───────────────────────────────────
$SERVICES = @(
    @{ Port = 8086; Name = "Saga Orchestrator"      },
    @{ Port = 8085; Name = "Notification Service"   },
    @{ Port = 8084; Name = "Payment Service"        },
    @{ Port = 8083; Name = "Booking Service"        },
    @{ Port = 8082; Name = "Flight Service"         },
    @{ Port = 8081; Name = "User Service"           },
    @{ Port = 8080; Name = "API Gateway"            },
    @{ Port = 8888; Name = "Config Server"          },
    @{ Port = 8761; Name = "Eureka Registry"        }
)

# ── Output helpers ────────────────────────────────────────────────────────────
function Write-OK {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}
function Write-Warn {
    param([string]$Message)
    Write-Host "  [--] $Message" -ForegroundColor DarkGray
}
function Write-Fail {
    param([string]$Message)
    Write-Host "  [XX] $Message" -ForegroundColor Red
}

# ── Stop the process listening on a given port ────────────────────────────────
function Stop-ServiceOnPort {
    param([int]$Port, [string]$Name)

    Write-Host "  Stopping $Name (port $Port)..." -ForegroundColor Cyan -NoNewline

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

    if ($null -eq $connections -or @($connections).Count -eq 0) {
        Write-Host "  not running." -ForegroundColor DarkGray
        return "skipped"
    }

    # Collect unique PIDs (multiple listen sockets can share one process)
    $ownerPids = @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
    $killed    = $false

    foreach ($pid in $ownerPids) {
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($null -eq $proc) { continue }

        Write-Host ""   # newline after the "Stopping..." prefix
        Write-Host "    PID $pid  ($($proc.ProcessName))" -ForegroundColor DarkGray
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            $killed = $true
        } catch {
            Write-Fail "Failed to stop PID $pid : $($_.Exception.Message)"
        }
    }

    if ($killed) {
        Write-OK "$Name stopped."
        # Brief pause so the OS releases the port before the next check
        Start-Sleep -Milliseconds 300
        return "stopped"
    }

    Write-Warn "${Name}: process found but could not be terminated."
    return "failed"
}

# ── Banner ────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host "   SkyWays Airlines  -  Stop All Services" -ForegroundColor Magenta
Write-Host "  =============================================" -ForegroundColor Magenta
Write-Host ""

# ── Stop each service ─────────────────────────────────────────────────────────
$stopped = 0
$skipped = 0
$failed  = 0

foreach ($svc in $SERVICES) {
    $result = Stop-ServiceOnPort -Port $svc.Port -Name $svc.Name
    switch ($result) {
        "stopped" { $stopped++ }
        "skipped" { $skipped++ }
        "failed"  { $failed++  }
    }
}

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "  ---------------------------------------------" -ForegroundColor DarkGray
Write-Host "  Stopped      : $stopped service(s)" -ForegroundColor Green
Write-Host "  Already down : $skipped service(s)" -ForegroundColor DarkGray
if ($failed -gt 0) {
    Write-Host "  Failed       : $failed service(s)" -ForegroundColor Red
}
Write-Host ""
Write-Host "  Note: PowerShell terminal windows opened by start-all-services.ps1" -ForegroundColor DarkGray
Write-Host "        remain open after the Java process exits. Close them manually." -ForegroundColor DarkGray
Write-Host ""