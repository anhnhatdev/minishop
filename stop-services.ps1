Write-Host "Stopping existing microservices..." -ForegroundColor Yellow

$ports = @(8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8761)

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($connections) {
        foreach ($conn in $connections) {
            $procId = $conn.OwningProcess
            if ($procId -gt 0) {
                Write-Host "Killing process $procId listening on port $port..." -ForegroundColor Red
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

Write-Host "All microservices stopped successfully!" -ForegroundColor Green
