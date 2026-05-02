$ErrorActionPreference = "Stop"

Write-Host "Stopping local stack..." -ForegroundColor Yellow
docker compose down

Write-Host "Done." -ForegroundColor Green
