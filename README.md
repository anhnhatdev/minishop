# MiniShop — E-commerce Microservices Platform

MiniShop is a modern, distributed e-commerce backend built with **Java 21**, **Spring Boot 3.x**, and **Spring Cloud**, designed following **Microservices Architecture** principles.

---

## 🏗️ System Architecture & Services

| Service | Port | Description | Tech Stack | Status |
|---|---|---|---|---|
| **User Service** | `8081` | Authentication, Authorization, JWT, Profile Management | Spring Boot 3.3, Security 6, PostgreSQL, Flyway, JJWT | ✅ Completed |
| **Product Service** | `8082` | Product Catalog, Categories, Inventory linkage | Spring Boot 3.3, PostgreSQL, Flyway | 🚧 Planned |
| **Eureka Server** | `8761` | Service Discovery & Registry | Spring Cloud Netflix Eureka | 🚧 Planned |
| **API Gateway** | `8080` | Unified API Entrypoint, Routing, Rate Limiting | Spring Cloud Gateway | 🚧 Planned |
| **Order Service** | `8083` | Order Processing, Saga Orchestration | Spring Boot 3.3, PostgreSQL, Kafka | 🚧 Planned |
| **Payment Service** | `8084` | Payment Gateways integration (VNPay/Momo/Stripe) | Spring Boot 3.3, PostgreSQL | 🚧 Planned |

---

## 🚀 User Service Highlights

- **Stateless Authentication**: JWT Access Tokens (HS256) + Hashed Refresh Tokens (SHA-256 in DB) with token rotation & revocation support.
- **Role-Based Access Control**: `ADMIN`, `SELLER`, `CUSTOMER`.
- **Database Migrations**: Version-controlled migrations via Flyway.
- **Containerization**: Multi-stage Docker build ready for Docker Compose / Kubernetes.
- **Interactive Documentation**: OpenAPI 3 / Swagger UI at `/swagger-ui.html`.

### API Endpoints (v1)

```http
POST   /api/v1/auth/register       # Register new user
POST   /api/v1/auth/login          # Authenticate & receive tokens
POST   /api/v1/auth/refresh        # Rotate refresh token & issue new access token
POST   /api/v1/auth/logout         # Revoke refresh token
GET    /api/v1/users/me            # Get current user profile (JWT required)
PUT    /api/v1/users/me            # Update user profile (JWT required)
GET    /api/v1/users               # Admin: List all users (ADMIN only)
PUT    /api/v1/users/{id}/status   # Admin: Update account status (ADMIN only)
```

---

## 🛠️ Tech Stack

- **Language & Runtime**: Java 21 LTS
- **Framework**: Spring Boot 3.3.2, Spring Security 6
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Object Mapping**: MapStruct 1.5.5
- **Security & Tokens**: JJWT 0.12.6, BCrypt (cost factor 12)
- **Documentation**: Springdoc OpenAPI 2.6.0
- **Containerization**: Docker (Eclipse Temurin Alpine)

---

## 📦 Getting Started

### Local Development

1. Navigate to the service folder:
   ```bash
   cd user-service
   ```
2. Run database migration and start service:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Access Swagger UI:
   ```
   http://localhost:8081/swagger-ui.html
   ```
