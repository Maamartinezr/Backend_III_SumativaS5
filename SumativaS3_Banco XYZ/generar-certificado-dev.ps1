$ErrorActionPreference = "Stop"

$certDir = Join-Path $PSScriptRoot "config"
$certPath = Join-Path $certDir "bankxyz-dev.p12"

if (-not (Test-Path $certDir)) {
    New-Item -ItemType Directory -Path $certDir | Out-Null
}

if (Test-Path $certPath) {
    Remove-Item -LiteralPath $certPath -Force
}

keytool `
    -genkeypair `
    -alias bankxyz-dev `
    -keyalg RSA `
    -keysize 2048 `
    -sigalg SHA256withRSA `
    -storetype PKCS12 `
    -keystore $certPath `
    -validity 365 `
    -storepass changeit `
    -keypass changeit `
    -dname "CN=localhost, OU=Backend III, O=Banco XYZ, L=Santiago, ST=RM, C=CL" `
    -ext "KU=digitalSignature,keyEncipherment" `
    -ext "EKU=serverAuth" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"

Write-Host "Certificado generado en: $certPath"
Write-Host "Para iniciar con HTTPS:"
Write-Host '.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=https"'
