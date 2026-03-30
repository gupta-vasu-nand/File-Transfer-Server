# docker-check.ps1
Write-Host "Checking Docker setup..." -ForegroundColor Cyan

# Check if container is running
$container = docker ps -q -f name=file-transfer-server
if ($container) {
    Write-Host "Container is running" -ForegroundColor Green
} else {
    Write-Host "Container is not running" -ForegroundColor Red
    exit
}

# Check storage mount
Write-Host "`nChecking storage mount..." -ForegroundColor Cyan
docker exec file-transfer-server ls -la /app/storage | Select-Object -First 10

# Check logs for errors
Write-Host "`nChecking logs..." -ForegroundColor Cyan
docker logs file-transfer-server --tail 20

# Test API endpoint
Write-Host "`nTesting API..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:9091/api/files" -UseBasicParsing
    Write-Host "API is accessible" -ForegroundColor Green
} catch {
    Write-Host "API is not accessible" -ForegroundColor Red
}