$ErrorActionPreference = "Stop"

Write-Host "[1/3] Ensuring local bind-mount folders exist..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "C:\Users\Javil\Desktop\Proyecto\WebAPP\tmp\mods-files" | Out-Null

Write-Host "[2/3] Validating docker compose config..." -ForegroundColor Cyan
docker compose config | Out-Null

Write-Host "[3/3] Starting local stack (db + backend + frontend)..." -ForegroundColor Cyan
docker compose up -d --build

Write-Host "`nLocal stack status:" -ForegroundColor Green
docker compose ps
