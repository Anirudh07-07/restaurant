# 🍽️ Kaveri Restaurant Group — Enterprise Food Ordering & Management Platform

> **A production-ready, full-stack Restaurant Management, Online Food Ordering, and Digital Operations Platform built with Spring Boot 3, Java 21 LTS, PostgreSQL 18 / MySQL, and Bootstrap 5. Features enterprise multi-tier security, sliding-window rate limiting, Razorpay payment gateway integration, and real-time Meta WhatsApp Business Cloud API two-way status synchronization.**

[![Build & Test](https://img.shields.io/badge/tests-56%20passed-brightgreen.svg)](#-automated-testing)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00.svg?logo=openjdk)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/PostgreSQL-18%20Managed-4169E1.svg?logo=postgresql)](https://www.postgresql.org/)
[![Live Status](https://img.shields.io/badge/Production-Live%20on%20Render-46E3B7.svg)](https://kaveri-restaurant.onrender.com)

---

## 🌐 Live Production Deployment

- **Public Storefront**: [https://kaveri-restaurant.onrender.com](https://kaveri-restaurant.onrender.com)
- **Digital Menu (242 Delicacies)**: [https://kaveri-restaurant.onrender.com/menu.html](https://kaveri-restaurant.onrender.com/menu.html)
- **Actuator Health Probe**: [https://kaveri-restaurant.onrender.com/actuator/health](https://kaveri-restaurant.onrender.com/actuator/health)
- **Kitchen Admin Console**: [https://kaveri-restaurant.onrender.com/admin/dashboard.html](https://kaveri-restaurant.onrender.com/admin/dashboard.html)

---

## 🚀 Key Feature Highlights

### 🛒 Customer Storefront & Ordering
- **Dual-Mode Digital Menu**: Fast toggle between Traditional Restaurant List Menu and Visual Food Cards.
- **Client & Server Search**: 180ms debounced search with category filter tabs and veg/spicy pills.
- **Secure Shopping Bag & Checkout**: Server-calculated taxes (5%), delivery fees (free over ₹500), and delivery addresses.
- **Razorpay Payments**: End-to-end payment order creation, client checkout signature verification, and automated refunds on cancellation. Includes an embedded sandbox mock simulator for zero-cost demos.
- **Table Reservations**: Date/time slot booking with party size validation and maximum 10-simultaneous reservation slot capacity limits.
- **Customer Order Timeline**: Real-time order progress tracking with visual timestamps.
- **In-App & WhatsApp Notifications**: Status alerts dispatched when kitchen accepts, cooks, dispatches, or delivers food.

### 👨‍🍳 Kitchen Maker & WhatsApp Operations
- **Two-Way Meta WhatsApp Cloud Synchronization**:
  - Outbound interactive button alerts sent to the kitchen maker when a customer places an order.
  - Makers can tap **Accept**, **Cook**, **Ready**, **Dispatch**, or **Deliver** directly from WhatsApp to update order status.
  - Customer receives matching WhatsApp template notifications automatically.
- **Transactional Outbox Pattern**: Delivery attempts are recorded in `whatsapp_notification_logs` with exponential retries so failed network calls never roll back committed orders.
- **Webhook Idempotency**: `whatsapp_webhook_events` ensures incoming webhook retries from Meta are processed strictly once.

### 🛡️ Multi-Tier Security Perimeter
- **Sliding-Window IP Rate Limiter**: Protects auth endpoints (10/min), payments (15/min), and public forms (120/min), returning `HTTP 429 Too Many Requests` with `Retry-After: 60`.
- **Strict Security Headers**: Enforces Content-Security-Policy (CSP), HSTS (`max-age=31536000`), `X-Frame-Options: SAMEORIGIN`, and `X-Content-Type-Options: nosniff`.
- **Synchronous Admin Guard**: Prevents unauthenticated access or UI layout leaks across administrative pages before DOM rendering.
- **IDOR Protection**: Order queries and payment verifications validate that the caller's JWT identity owns the targeted records.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Spring Boot 3.3.4 on Java 21 LTS |
| **Persistence / ORM** | Spring Data JPA / Hibernate 6.5.3 |
| **Production Database** | Managed PostgreSQL 18 (Render Cloud DB) |
| **Development Database** | PostgreSQL, MySQL 8.0, or H2 In-Memory |
| **Security & Auth** | Spring Security 6, JJWT 0.12.5 (HMAC-SHA256) |
| **Payment Gateway** | Razorpay Java SDK 1.4.3 + Webhooks |
| **Messaging Integration** | Meta WhatsApp Business Platform Cloud API (Graph API v20.0) |
| **Frontend** | HTML5, CSS3, ES6 JavaScript, Bootstrap 5.3.2, FontAwesome 6 |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc (56 tests) |
| **Containerization** | Docker multi-stage build (`eclipse-temurin:21-jre-alpine`) |

---

## 🏃 Local Quickstart & Development

### Prerequisites
- **JDK 21 LTS** (`JAVA_HOME=/opt/homebrew/opt/openjdk@21` on macOS)
- **Maven 3.9+**

### 1. Run in In-Memory H2 Mode (Zero DB Setup Required)
```bash
cd foodnest
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Access the application locally at: **`http://localhost:8080`**

### 2. Default Seeded Credentials
- **Admin**: `admin@thekaveri.com` / `Admin@1234`
- **Customer**: `customer@thekaveri.com` / `Customer@1234`

---

## 🧪 Automated Testing

Run the full automated test suite (56 tests / 0 failures):
```bash
cd foodnest
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean test
```

See [`docs/TESTING.md`](docs/TESTING.md) for full test matrix and coverage details.

---

## ☁️ Production Deployment

### 1-Click Render Blueprint Deployment
The repository includes a ready-to-run [`render.yaml`](render.yaml) Blueprint:
1. Connect your GitHub repository to [dashboard.render.com](https://dashboard.render.com).
2. Create a **New Blueprint Instance**, select branch `client/kaveri`.
3. Render automatically provisions the PostgreSQL database (`kaveridb`), builds the Docker container, wires environment credentials, and publishes your HTTPS URL.

See [`deployment/DEPLOY_GUIDE.md`](deployment/DEPLOY_GUIDE.md) and [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md) for complete production operations.
