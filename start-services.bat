@echo off
echo ========================================================
echo Launching MiniShop Microservices in Sequence...
echo ========================================================

echo [1/9] Starting Eureka Server (Port 8761)...
start "Eureka Server [8761]" /D "%~dp0eureka-server" cmd /k "mvnw.cmd spring-boot:run"
echo Waiting 10 seconds for Eureka Server to initialize...
timeout /t 10 /nobreak >nul

echo [2/9] Starting User Service (Port 8081)...
start "User Service [8081]" /D "%~dp0user-service" cmd /k "mvnw.cmd spring-boot:run"

echo [3/9] Starting Product Service (Port 8082)...
start "Product Service [8082]" /D "%~dp0product-service" cmd /k "mvnw.cmd spring-boot:run"

echo [4/9] Starting Inventory Service (Port 8085)...
start "Inventory Service [8085]" /D "%~dp0inventory-service" cmd /k "mvnw.cmd spring-boot:run"

echo [5/9] Starting Payment Service (Port 8084)...
start "Payment Service [8084]" /D "%~dp0payment-service" cmd /k "mvnw.cmd spring-boot:run"

echo [6/9] Starting Notification Service (Port 8086)...
start "Notification Service [8086]" /D "%~dp0notification-service" cmd /k "mvnw.cmd spring-boot:run"

echo [7/9] Starting Review Service (Port 8087)...
start "Review Service [8087]" /D "%~dp0review-service" cmd /k "mvnw.cmd spring-boot:run"

echo [8/9] Starting Order Service (Port 8083)...
start "Order Service [8083]" /D "%~dp0order-service" cmd /k "mvnw.cmd spring-boot:run"

echo Waiting 10 seconds before launching API Gateway...
timeout /t 10 /nobreak >nul

echo [9/9] Starting API Gateway (Port 8080)...
start "API Gateway [8080]" /D "%~dp0api-gateway" cmd /k "mvnw.cmd spring-boot:run"

echo.
echo ========================================================
echo All services launched!
echo - Eureka Registry Dashboard: http://localhost:8761
echo - API Gateway Entrypoint:   http://localhost:8080
echo ========================================================
pause
