#Requires -Version 5.1
<#
.SYNOPSIS
  Crea el workspace Angular de La Viñuela: dos aplicaciones (tienda, panel) y cuatro librerías
  (ui, core, api, util), y aplica la configuración estricta del proyecto.
  Equivale a tools/bootstrap-frontend.sh para Windows PowerShell.
.NOTES
  Requiere Node.js LTS y acceso a internet. Uso:  .\tools\bootstrap-frontend.ps1
                                                  .\tools\bootstrap-frontend.ps1 -AngularMajor 23
#>
param([string]$AngularMajor = '22')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

if (Test-Path frontend) {
    throw "La carpeta 'frontend' ya existe. Bórrala o renómbrala si quieres volver a generarla."
}
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Falta Node.js (versión LTS).' }

# Se usa npx.cmd (no npx) para evitar problemas con la política de ejecución de scripts de PowerShell.
function Invoke-Native {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Ha fallado: $Command $($Arguments -join ' ')" }
}

Write-Host ">> Creando el workspace con Angular $AngularMajor..."
Invoke-Native 'npx.cmd' @('--yes', "@angular/cli@$AngularMajor", 'new', 'frontend',
    '--create-application=false', '--package-manager=npm', '--skip-git', '--interactive=false')

Set-Location frontend

Write-Host '>> Aplicaciones...'
Invoke-Native 'npx.cmd' @('ng', 'generate', 'application', 'tienda', '--prefix=lv', '--style=scss', '--routing', '--ssr=false', '--interactive=false')
Invoke-Native 'npx.cmd' @('ng', 'generate', 'application', 'panel', '--prefix=lv', '--style=scss', '--routing', '--ssr=false', '--interactive=false')

Write-Host '>> Librerías...'
foreach ($lib in 'ui', 'core', 'api', 'util') {
    Invoke-Native 'npx.cmd' @('ng', 'generate', 'library', $lib, '--prefix=lv', '--interactive=false')
}

Write-Host '>> Configuración estricta del proyecto...'
Invoke-Native 'node' @('..\tools\apply-frontend-config.mjs')

Write-Host ''
Write-Host 'Listo. Comprueba que compila:  cd frontend; npx.cmd ng build tienda; npx.cmd ng build panel'
Write-Host 'Siguientes pasos en docs\frontend-bootstrap.md'
