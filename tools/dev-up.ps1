#Requires -Version 5.1
<#
.SYNOPSIS
  Levanta el entorno local (MySQL, Valkey y Mailpit) y deja listo el fichero .env.
  Equivale a tools/dev-up.sh para Windows PowerShell.
.NOTES
  La primera vez crea .env con contraseñas aleatorias. Requiere Docker Desktop en marcha.
#>
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'No encuentro Docker. Instala Docker Desktop y ábrelo antes de continuar.'
}

function New-RandomPassword {
    param([int]$Length = 24)
    $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

if (-not (Test-Path .env)) {
    Write-Host '>> Creando .env con contraseñas aleatorias...'
    $lines = Get-Content .env.example | ForEach-Object {
        if ($_ -match '^(MYSQL_ROOT_PASSWORD|DB_MIGRATE_PASSWORD|DB_APP_PASSWORD|DB_RO_PASSWORD)=') {
            '{0}={1}' -f $Matches[1], (New-RandomPassword)
        } else {
            $_
        }
    }
    # UTF-8 sin BOM: con BOM, Docker Compose leería mal el nombre de la primera variable.
    $path = Join-Path (Get-Location) '.env'
    [System.IO.File]::WriteAllLines($path, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false)))
}

# Clave secreta de los códigos de verificación de correo. Si .env ya existía de antes, se añade ahora.
$envPath = Join-Path (Get-Location) '.env'
if (-not (Select-String -Path $envPath -Pattern '^APP_CODE_PEPPER=' -Quiet)) {
    $pepper = "`nAPP_CODE_PEPPER=" + (New-RandomPassword 48) + "`n"
    [System.IO.File]::AppendAllText($envPath, $pepper, (New-Object System.Text.UTF8Encoding($false)))
}

Write-Host '>> Arrancando contenedores...'
docker compose up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose ha fallado. Revisa que Docker Desktop esté abierto.' }

$vars = @{}
Get-Content .env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    $vars[$k] = $v
}

Write-Host ''
Write-Host 'Entorno listo:'
Write-Host ('  MySQL    127.0.0.1:{0}   (base de datos: {1})' -f $vars['DB_PORT'], $vars['DB_NAME'])
Write-Host ('  Valkey   127.0.0.1:{0}' -f $vars['REDIS_PORT'])
Write-Host ('  Mailpit  http://127.0.0.1:{0}   (SMTP en el puerto {1})' -f $vars['MAIL_UI_PORT'], $vars['MAIL_SMTP_PORT'])
Write-Host ''
Write-Host 'Para arrancar la API con ese entorno:'
Write-Host '  .\tools\load-env.ps1'
Write-Host '  cd backend'
Write-Host '  mvn spring-boot:run "-Dspring-boot.run.profiles=local"'
