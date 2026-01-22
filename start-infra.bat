@echo off
echo ========================================================
echo Starting MiniShop Infrastructure (Postgres, Mongo, Kafka, Mailpit)...
echo ========================================================
docker compose up -d
echo.
echo Infrastructure started!
echo - Postgres: localhost:5432 (user_db, product_db, order_db, inventory_db, payment_db, notification_db)
echo - MongoDB:  localhost:27017 (review_db)
echo - Kafka:    localhost:9092
echo - Mailpit:  http://localhost:8025 (Web UI Email Inbox)
pause
