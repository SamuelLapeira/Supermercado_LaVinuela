#Requires -Version 5.1
<#
.SYNOPSIS
  Para el entorno local. Con -Reset borra también los datos (el volumen de MySQL).
#>
param([switch]$Reset)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if ($Reset) {
    Write-Host '>> Parando y BORRANDO los datos locales...'
    docker compose down --volumes
} else {
    Write-Host '>> Parando contenedores (los datos se conservan)...'
    docker compose down
}
