# 🛍️ MiniShop — E-Commerce Microservices Platform

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![Spring Boot 3.3.2](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2023.0.3](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-cloud)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Distributed%20Events-231F20.svg?style=for-the-badge&logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database%20Per%20Service-336791.svg?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage%20Containers-2496ED.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)

**MiniShop** is an enterprise-grade, distributed e-commerce backend platform built with **Java 21**, **Spring Boot 3.3**, **Spring Cloud 2023**, and **Apache Kafka**. Designed to mirror high-throughput production e-commerce architectures (e.g., Shopee, Lazada), it implements core distributed patterns including **Choreography-based Saga**, **Anti-Oversell Optimistic Locking Concurrency Control**, **Payment Gateway HMAC-SHA512 Verification**, **Asynchronous Multi-channel Notifications**, **Stateless JWT Security**, **Service Discovery**, **Resilience4j Circuit Breakers**, and **Database-per-Service** isolation.

---

## 🏛️ System Architecture

```mermaid
graph TD
    Client(["📱 Web / Mobile Clients"])
    
    subgraph Edge Layer
        Gateway["🛡️ API Gateway (Port 8080)<br/>• Non-blocking Netty<br/>• JWT Pre-Validation Filter<br/>• Circuit Breakers & CORS<br/>• Request Tracing (X-Trace-Id)"]
    end
    
    subgraph Service Discovery
        Eureka["🧭 Eureka Server (Port 8761)<br/>• Dynamic Service Registry<br/>• Realtime Instance Health Heartbeats"]
    end
    
    subgraph Core Microservices
        UserService["👤 User Service (Port 8081)<br/>• Auth, JWT & Token Rotation<br/>• User Profiles & Admin Status"]
        ProductService["📦 Product Service (Port 8082)<br/>• Catalog & Hierarchical Categories<br/>• Dynamic Filtering & Multi-spec"]
        OrderService["🛒 Order Service (Port 8083)<br/>• Shopping Cart Snapshots<br/>• Order State Machine<br/>• Saga Orchestrator & Compensation"]
        InventoryService["📊 Inventory Service (Port 8085)<br/>• Anti-Oversell Optimistic Locking<br/>• Append-Only Movements Audit<br/>• Saga Reservation Handlers"]
        PaymentService["💳 Payment Service (Port 8084)<br/>• VNPay Sandbox & COD Settlement<br/>• HMAC-SHA512 Signature Security<br/>• IPN Webhook & Raw Audit Logs"]
        NotificationService["🔔 Notification Service (Port 8086)<br/>• Multi-channel Email / SMS Delivery<br/>• Database Template Placeholder Engine<br/>• Audit Logs & Scheduled Auto-retry"]
    end
    
    subgraph Event Broker
        Kafka{{"📨 Apache Kafka<br/>Topics: order.created, stock.reserved, payment.succeeded, order.confirmed, etc."}}
    end

    subgraph Data Stores
        UserDB[("🗄️ user_db (PostgreSQL)")]
        ProductDB[("🗄️ product_db (PostgreSQL)")]
        OrderDB[("🗄️ order_db (PostgreSQL)")]
        InventoryDB[("🗄️ inventory_db (PostgreSQL)")]
        PaymentDB[("🗄️ payment_db (PostgreSQL)")]
        NotificationDB[("🗄️ notification_db (PostgreSQL)")]
    end

    Client -->|HTTP / REST| Gateway
    Gateway -->|Discovery Lookup| Eureka
    Gateway -->|Load-Balanced lb://| UserService
    Gateway -->|Load-Balanced lb://| ProductService
    Gateway -->|Load-Balanced lb://| OrderService
    Gateway -->|Load-Balanced lb://| InventoryService
    Gateway -->|Load-Balanced lb://| PaymentService
    Gateway -->|Load-Balanced lb://| NotificationService
    
    OrderService -.->|Synchronous Feign Price Snapshot| ProductService
    OrderService <===>|Publish / Consume Events| Kafka
    InventoryService <===>|Publish / Consume Events| Kafka
    PaymentService <===>|Publish / Consume Events| Kafka
    NotificationService <===|Pure Asynchronous Consumer| Kafka
    
    UserService --> UserDB
    ProductService --> ProductDB
    OrderService --> OrderDB
    InventoryService --> InventoryDB
    PaymentService --> PaymentDB
    NotificationService --> NotificationDB
```

---

## 📦 Microservices Catalog

| Service | Port | Database | Primary Responsibilities | Status |
|---|---|---|---|---|
| **[Eureka Server](./eureka-server)** | `8761` | *None* | Centralized Service Registry, Dynamic Discovery, Health Checks | ✅ Active |
| **[API Gateway](./api-gateway)** | `8080` | *None* | Reactive Entrypoint, Route Management, JWT Filter, Circuit Breakers, CORS | ✅ Active |
| **[User Service](./user-service)** | `8081` | `user_db` (PostgreSQL) | Authentication, Token Rotation, Role-based Auth (`ADMIN`, `SELLER`, `CUSTOMER`) | ✅ Active |
| **[Product Service](./product-service)** | `8082` | `product_db` (PostgreSQL) | Product Catalog, Category Tree, Specification Search, Owner Authorization | ✅ Active |
| **[Order Service](./order-service)** | `8083` | `order_db` (PostgreSQL) | Shopping Cart, Order State Machine, OpenFeign, Kafka Saga Orchestration | ✅ Active |
| **[Inventory Service](./inventory-service)** | `8085` | `inventory_db` (PostgreSQL) | Anti-Oversell Optimistic Locking (`@Version`), Saga Event Handlers, Append-only Audit | ✅ Active |
| **[Payment Service](./payment-service)** | `8084` | `payment_db` (PostgreSQL) | VNPay Sandbox Gateway, HMAC-SHA512 Signature Security, IPN Server Webhook, Raw Audit Logs | ✅ Active |
| **[Notification Service](./notification-service)** | `8086` | `notification_db` (PostgreSQL) | Multi-channel Delivery (Email/SMS), Template Placeholder Engine, Audit Logs, Auto-Retry | ✅ Active |

---

## ⚡ Choreography-based Saga Orchestration

The platform implements a distributed **Choreography-based Saga pattern** to maintain data consistency across services without two-phase commit (2PC) locks.

```text
1. Order Creation:
   [Client] POST /api/v1/orders/checkout ──► [Order Service]
                                                  │
                                                  ├─► Saves Order (PENDING) & captures immutable snapshots
                                                  ├─► Clears Shopping Cart
                                                  └─► Publishes "order.created" to Kafka

2. Stock Reservation (Inventory Service - Optimistic Locking):
   [Kafka] "order.created" ──► [Inventory Service]
                                      │
                                      ├── (Success: all items available) ──► Publishes "stock.reserved"
                                      └── (Failure: out of stock)        ──► Publishes "stock.rejected"

3. Order State Advance / Payment Trigger:
   [Kafka] "stock.reserved" ──► [Order Service]
                                      │
                                      ├─► Status: STOCK_RESERVED
                                      └─► Publishes "payment.requested"

4. Payment Processing (Payment Service):
   [Kafka] "payment.requested" ──► [Payment Service]
                                         │
                                         ├── (COD) ────► Status: SUCCESS ──► Publishes "payment.succeeded"
                                         ├── (VNPAY) ──► Generates Sandbox Checkout URL (Status: PENDING)
                                         │                 │
                                         │                 ▼
                                         │               [VNPay Server IPN Callback]
                                         │                 ├── (00 Success) ──► Publishes "payment.succeeded"
                                         │                 └── (Failed)     ──► Publishes "payment.failed"

5. Order Confirmation & Compensating Transactions:
   [Kafka] "payment.succeeded" ──► [Order Service] ──────► Status: CONFIRMED ──► Publishes "order.confirmed"
                                   [Inventory Service] ──► Deducts real stock & publishes "inventory.updated"
                                   [Notification Service] ─► Renders ORDER_CONFIRMED & Dispatches Email

   [Kafka] "payment.failed"    ──► [Order Service] ──────► Status: CANCELLED ──► Publishes "order.cancelled"
                                   [Inventory Service] ──► Releases reserved stock & publishes "inventory.updated"
                                   [Notification Service] ─► Renders ORDER_CANCELLED / PAYMENT_FAILED & Dispatches Email

6. Safety Timeout Workers:
   [OrderTimeoutScheduler] in Order Service scans for stuck orders in STOCK_RESERVED > 15 mins ──► Triggers compensation
   [StockReservationTimeoutJob] in Inventory Service releases orphaned reservations > 15 mins
   [PaymentTimeoutScheduler] in Payment Service expires pending payments > 15 mins and publishes "payment.failed"
   [NotificationRetryScheduler] in Notification Service retries failed messages up to 3 times
```

---

## 🛡️ Key Architectural Patterns & Features

- **Database per Service**: Zero direct table joins across services. Cross-service data is communicated via synchronous OpenFeign DTOs or asynchronous Kafka domain events.
- **Asynchronous Side-Effect Isolation**: Notification delivery failures never throw unhandled exceptions back to Kafka or block core checkout/payment workflows.
- **Database-Driven Notification Templates**: Templates are stored in `notification_templates` for zero-downtime content edits, supporting dynamic `{{placeholder}}` token rendering.
- **Cryptographic Gateway Security**: VNPay callbacks are strictly validated with HMAC-SHA512. Return URLs are strictly decoupled from authoritative server-to-server IPN webhooks.
- **Raw Callback & Notification Audit Trails**: Verbatim payloads from gateways are saved in `payment_callback_logs` and all delivered notifications are tracked in `notification_logs`.
- **Optimistic Locking Anti-Oversell Control**: JPA `@Version` on `Inventory` combined with Spring Retry (`@Retryable`) ensures multiple concurrent purchases on scarce items never result in overselling.
- **Append-Only Movement Audit Trail**: Every stock change (`IMPORT`, `RESERVE`, `DEDUCT`, `RELEASE`, `ADJUST`) is permanently logged into `stock_movements` for audit and reconciliation.
- **Immutable Price & Name Snapshots**: When adding items to cart or checking out, `order-service` captures snapshots of product name and price. Future seller updates never alter historical orders.
- **Idempotent Consumers**: Consumers verify message uniqueness against `processed_events` before executing business logic, protecting against duplicate deliveries.
- **Stateless Authentication**: JJWT 0.12.6 tokens with SHA-256 hashed refresh tokens, token rotation on refresh, and instant revocation on logout.
- **Reactive API Gateway**: Non-blocking Spring Cloud Gateway filtering requests, attaching `X-Trace-Id` headers, extracting user claims (`X-User-Id`, `X-User-Role`), and providing Resilience4j 503 fallback handlers.

---

## 📡 API Catalog Overview

### 👤 User Service (`8081` / via Gateway `8080`)
```http
POST   /api/v1/auth/register          # Register user (CUSTOMER / SELLER)
POST   /api/v1/auth/login             # Authenticate & issue Access + Refresh tokens
POST   /api/v1/auth/refresh           # Rotate refresh token & get new access token
POST   /api/v1/auth/logout            # Revoke refresh token
GET    /api/v1/users/me               # Get current user profile (JWT)
PUT    /api/v1/users/me               # Update current profile (JWT)
GET    /api/v1/users                  # Admin: List users with pagination & filtering
PUT    /api/v1/users/{id}/status      # Admin: Lock/Activate user account
```

### 📦 Product Service (`8082` / via Gateway `8080`)
```http
GET    /api/v1/products               # Public: Dynamic multi-criteria search & pagination
GET    /api/v1/products/{id}          # Public: Product detail with image gallery
POST   /api/v1/products               # Seller/Admin: Create new product
PUT    /api/v1/products/{id}          # Owner/Admin: Update product details
DELETE /api/v1/products/{id}          # Owner/Admin: Soft delete product (HIDDEN)
GET    /api/v1/categories             # Public: Category tree hierarchy
POST   /api/v1/categories             # Admin: Create category
```

### 🛒 Order Service (`8083` / via Gateway `8080`)
```http
GET    /api/v1/cart                   # Get current user's shopping cart & subtotal
POST   /api/v1/cart/items             # Add product with real-time price snapshot
PUT    /api/v1/cart/items/{itemId}    # Update item quantity
DELETE /api/v1/cart/items/{itemId}    # Remove item from cart
DELETE /api/v1/cart                   # Clear cart
POST   /api/v1/orders/checkout        # Checkout cart & launch Saga workflow
GET    /api/v1/orders                 # User: Order history with status filter
GET    /api/v1/orders/{id}            # User/Admin: Order detail & audit status history
POST   /api/v1/orders/{id}/cancel     # User: Cancel order (triggers compensation)
PUT    /api/v1/orders/{id}/status     # Seller/Admin: Advance order lifecycle
```

### 📊 Inventory Service (`8085` / via Gateway `8080`)
```http
POST   /api/v1/inventory/import          # Admin/Seller: Import stock with batch note
GET    /api/v1/inventory/{productId}     # Admin/Seller: View inventory & version numbers
PUT    /api/v1/inventory/{productId}/adjust # Admin: Manually adjust stock with audit note
GET    /api/v1/inventory/{productId}/movements # Admin: View append-only audit trail
```

### 💳 Payment Service (`8084` / via Gateway `8080`)
```http
GET    /api/v1/payments/{orderId}/status       # Poll payment transaction status & URL
GET    /api/v1/payments/{orderId}/callback-logs # Admin: View raw gateway callback audit logs
GET    /api/v1/payments/vnpay/return          # Public: Non-authoritative return display
GET    /api/v1/payments/vnpay/ipn             # Public: Authoritative Server-to-Server IPN webhook
POST   /api/v1/payments/vnpay/ipn             # Public: Authoritative Server-to-Server IPN webhook
```

### 🔔 Notification Service (`8086` / via Gateway `8080`)
```http
GET    /api/v1/notifications/logs             # Admin: Query notification delivery logs with filters
POST   /api/v1/notifications/{logId}/resend   # Admin: Manually trigger redelivery of failed notification
GET    /api/v1/notifications/templates        # Admin: List all notification templates & tokens
```

---

## 🛠️ Technology Stack & Dependencies

- **Language & JDK**: Java 21 LTS (Eclipse Temurin)
- **Frameworks**: Spring Boot 3.3.2, Spring Cloud 2023.0.3 (Gateway, Netflix Eureka, OpenFeign)
- **Event Messaging**: Apache Kafka 3.x with Spring Kafka
- **Mailing & Communication**: Spring Boot Mail (`JavaMailSender`), Jakarta Mail
- **Cryptography & Security**: HMAC-SHA512 Signature Verification, Spring Security 6, JJWT 0.12.6, BCrypt (cost factor 12)
- **Concurrency & Resilience**: JPA `@Version` Optimistic Locking, Spring Retry (`@Retryable`), Resilience4j Circuit Breaker
- **Persistence & Migration**: PostgreSQL 16, Spring Data JPA, Hibernate 6, Flyway Migrations
- **Mapping & Utilities**: MapStruct 1.5.5, Lombok
- **API Documentation**: Springdoc OpenAPI 2.6.0 (Swagger UI at `/swagger-ui.html`)
- **Containerization**: Multi-stage Dockerfiles with healthchecks

---

## 🚀 Getting Started

### 1. Prerequisites
- **JDK 21 LTS**
- **Docker & Docker Compose** (for PostgreSQL & Kafka)
- **Maven 3.9+** (or using bundled `./mvnw`)

### 2. Running Services Locally

Start the services in the following order:

```bash
# 1. Start Eureka Discovery Server
cd eureka-server && ./mvnw spring-boot:run

# 2. Start Core Services
cd ../user-service && ./mvnw spring-boot:run
cd ../product-service && ./mvnw spring-boot:run
cd ../order-service && ./mvnw spring-boot:run
cd ../inventory-service && ./mvnw spring-boot:run
cd ../payment-service && ./mvnw spring-boot:run
cd ../notification-service && ./mvnw spring-boot:run

# 3. Start API Gateway
cd ../api-gateway && ./mvnw spring-boot:run
```

### 3. Service Dashboards & Swagger UIs
- **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761)
- **API Gateway Entry**: [http://localhost:8080](http://localhost:8080)
- **User Service Swagger**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Product Service Swagger**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Order Service Swagger**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- **Inventory Service Swagger**: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- **Payment Service Swagger**: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- **Notification Service Swagger**: [http://localhost:8086/swagger-ui.html](http://localhost:8086/swagger-ui.html)

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
