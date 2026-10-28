#Requires -Version 5.1
<#
.SYNOPSIS
  Carga las variables del fichero .env en la ventana de PowerShell actual
  (equivale a "set -a; source .env; set +a" de Linux). Hay que ejecutarlo en la misma ventana
  desde la que después se lanza la aplicación.
#>
$ErrorActionPreference = 'Stop'
$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if (-not (Test-Path $envFile)) {
    throw 'No existe el fichero .env. Ejecuta primero .\tools\dev-up.ps1'
}
Get-Content $envFile | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($k, $v, 'Process')
}
Write-Host 'Variables de .env cargadas en esta ventana.'
