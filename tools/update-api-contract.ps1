#Requires -Version 5.1
<#
.SYNOPSIS
  Actualiza el contrato de la API y el cliente de Angular en un solo paso.
.DESCRIPTION
  1. Arranca la aplicación en una prueba y guarda el contrato que genera en contracts\openapi.json (necesita Docker).
  2. Regenera el cliente de Angular a partir de ese contrato.
  Después hay que subir a Git los dos cambios juntos.
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

Write-Host '>> 1/2 Generando el contrato desde el servidor (necesita Docker abierto)...'
Set-Location (Join-Path $root 'backend')
mvn test "-Dtest=OpenApiContractTests" "-Dopenapi.update=true"
if ($LASTEXITCODE -ne 0) { throw 'No se ha podido generar el contrato. Revisa el error de arriba.' }

Write-Host '>> 2/2 Regenerando el cliente de Angular...'
Set-Location (Join-Path $root 'frontend')
npm run api:generate
if ($LASTEXITCODE -ne 0) { throw 'No se ha podido generar el cliente de Angular.' }

Set-Location $root
Write-Host ''
Write-Host 'Listo. Revisa los cambios con "git status" y súbelos juntos: contracts\openapi.json y frontend\projects\api.'
