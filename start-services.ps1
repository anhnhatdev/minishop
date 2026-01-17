Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Launching MiniShop Microservices in Sequence (PowerShell)..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$root = $PSScriptRoot

Write-Host "[1/9] Starting Eureka Server (Port 8761)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\eureka-server"
Write-Host "Waiting 10 seconds for Eureka Server to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "[2/9] Starting User Service (Port 8081)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\user-service"

Write-Host "[3/9] Starting Product Service (Port 8082)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\product-service"

Write-Host "[4/9] Starting Inventory Service (Port 8085)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\inventory-service"

Write-Host "[5/9] Starting Payment Service (Port 8084)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\payment-service"

Write-Host "[6/9] Starting Notification Service (Port 8086)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\notification-service"

Write-Host "[7/9] Starting Review Service (Port 8087)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\review-service"

Write-Host "[8/9] Starting Order Service (Port 8083)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\order-service"

Write-Host "Waiting 10 seconds before launching API Gateway..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "[9/9] Starting API Gateway (Port 8080)..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k", "mvnw.cmd spring-boot:run" -WorkingDirectory "$root\api-gateway"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "All services launched!" -ForegroundColor Green
Write-Host "- Eureka Registry Dashboard: http://localhost:8761" -ForegroundColor White
Write-Host "- API Gateway Entrypoint:   http://localhost:8080" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor Cyan
