param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$WebToken = "web-token-banco-xyz",
    [string]$MobileToken = "mobile-token-banco-xyz",
    [string]$AtmToken = "atm-token-banco-xyz",
    [long]$CuentaId = 103
)

$ErrorActionPreference = "Stop"

function Invoke-BffMetric {
    param(
        [string]$Canal,
        [string]$Uri,
        [string]$Token,
        [string]$Method = "Get",
        [string]$Body = $null
    )

    $headers = @{ "X-BFF-Token" = $Token }
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    if ($Body) {
        $response = Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers -ContentType "application/json" -Body $Body
    } else {
        $response = Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers
    }

    $stopwatch.Stop()
    $json = $response | ConvertTo-Json -Depth 10 -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetByteCount($json)

    [PSCustomObject]@{
        canal = $Canal
        metodo = $Method.ToUpper()
        endpoint = $Uri
        duracion_ms = $stopwatch.ElapsedMilliseconds
        payload_bytes = $bytes
        payload_kb = [Math]::Round($bytes / 1024, 2)
        fecha_medicion = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }
}

$metricas = @()
$metricas += Invoke-BffMetric -Canal "WEB" -Uri "$BaseUrl/api/bff/web/dashboard" -Token $WebToken
$metricas += Invoke-BffMetric -Canal "MOBILE" -Uri "$BaseUrl/api/bff/mobile/resumen" -Token $MobileToken
$metricas += Invoke-BffMetric -Canal "ATM_CONSULTA" -Uri "$BaseUrl/api/bff/atm/cuentas/$CuentaId" -Token $AtmToken
$metricas += Invoke-BffMetric -Canal "ATM_RETIRO" -Uri "$BaseUrl/api/bff/atm/cuentas/$CuentaId/retiros" -Token $AtmToken -Method "Post" -Body '{"monto":10000}'

$evidenciasDir = Join-Path $PSScriptRoot "target\evidencias"
if (-not (Test-Path $evidenciasDir)) {
    New-Item -ItemType Directory -Path $evidenciasDir | Out-Null
}

$jsonPath = Join-Path $evidenciasDir "bff-metricas.json"
$csvPath = Join-Path $evidenciasDir "bff-metricas.csv"

$metricas | ConvertTo-Json -Depth 10 | Set-Content -Path $jsonPath -Encoding UTF8
$metricas | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

$metricas | Format-Table -AutoSize
Write-Host ""
Write-Host "Evidencia JSON: $jsonPath"
Write-Host "Evidencia CSV : $csvPath"
