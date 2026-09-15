param(
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputDir = Join-Path -Path "target" -ChildPath "evidencias"
$outputFile = Join-Path -Path $outputDir -ChildPath "evidencia-api-$timestamp.json"

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

function Invoke-BankJob {
    param(
        [string]$Nombre,
        [string]$Uri
    )

    Write-Host ""
    Write-Host "=== Ejecutando $Nombre ===" -ForegroundColor Cyan
    $resultado = Invoke-RestMethod -Method Post -Uri $Uri
    $resultado | ConvertTo-Json -Depth 5
    return $resultado
}

function Get-Evidencia {
    param(
        [string]$Nombre,
        [string]$Uri
    )

    Write-Host ""
    Write-Host "=== Consultando $Nombre ===" -ForegroundColor Green
    $resultado = Invoke-RestMethod -Uri $Uri
    $resultado | ConvertTo-Json -Depth 8
    return $resultado
}

$evidencia = [ordered]@{
    fecha = (Get-Date).ToString("s")
    baseUrl = $BaseUrl
    ejecuciones = [ordered]@{
        transacciones = Invoke-BankJob "Job 1 - Transacciones diarias" "$BaseUrl/api/transacciones/procesar"
        intereses = Invoke-BankJob "Job 2 - Intereses mensuales" "$BaseUrl/api/intereses/procesar"
        cuentasAnuales = Invoke-BankJob "Job 3 - Cuentas anuales" "$BaseUrl/api/cuentas-anuales/procesar"
    }
    metricas = [ordered]@{
        jobs = Get-Evidencia "metricas de Jobs" "$BaseUrl/api/resultados/evidencia/jobs"
        steps = Get-Evidencia "metricas de Steps" "$BaseUrl/api/resultados/evidencia/steps"
        resultadosOficiales = Get-Evidencia "conteo de resultados oficiales" "$BaseUrl/api/resultados/evidencia/resultados-oficiales"
    }
    resultados = [ordered]@{
        resumenTransacciones = Get-Evidencia "resumen de transacciones" "$BaseUrl/api/resultados/transacciones/resumen"
        interesesProcesados = Get-Evidencia "intereses procesados" "$BaseUrl/api/resultados/intereses"
        resumenCuentasAnuales = Get-Evidencia "resumen de cuentas anuales" "$BaseUrl/api/resultados/cuentas-anuales/resumen"
    }
}

$evidencia | ConvertTo-Json -Depth 10 | Set-Content -Path $outputFile -Encoding UTF8

Write-Host ""
Write-Host "Evidencia guardada en $outputFile" -ForegroundColor Yellow
