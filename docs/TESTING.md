# Kaveri Restaurant — Automated Testing Guide & Test Matrix

**Last Updated**: October 21, 2026  
**Test Suite Status**: **56 Tests Passing (100% Success)**  
**Execution Environment**: Java 21 LTS (`/opt/homebrew/opt/openjdk@21`), Maven Surefire 3.2.5, JUnit 5, Mockito

---

## 1. Quick Test Execution Commands

```bash
# Run entire test suite using OpenJDK 21 LTS
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean test -f foodnest/pom.xml

# Run only security rate-limiting tests
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test -Dtest=RateLimitingFilterTest -f foodnest/pom.xml

# Run payment verification and refund tests
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test -Dtest=PaymentServiceTest,PaymentControllerTest -f foodnest/pom.xml

# Run WhatsApp webhook & notification outbox tests
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test -Dtest=WhatsAppWebhookControllerTest,WhatsAppWebhookServiceTest,WhatsAppNotificationServiceTest -f foodnest/pom.xml
```

---

## 2. Test Architecture & Profiles

- **Profile**: Activated via `@ActiveProfiles("test")`, loading `src/test/resources/application-test.properties`.
- **Database**: In-memory H2 (`jdbc:h2:mem:kaveri_testdb;DB_CLOSE_DELAY=-1;MODE=MySQL`), completely isolated from persistent storage.
- **External Mocking**:
  - `Razorpay`: Simulated via `razorpay.mock.enabled=true` with instant cryptographic mock IDs (`rfnd_mock_...`, `order_mock_...`).
  - `Meta WhatsApp`: Outbox and interactive button delivery simulated without external HTTP network calls.
  - `Email / Delivery`: Zero external billable or rate-limited service invocations during automated tests.

---

## 3. Test Inventory Matrix

| Category | Test Class | Tests | Status | Key Assertions Covered |
| :--- | :--- | :---: | :---: | :--- |
| **Security & Rate Limiting** | `RateLimitingFilterTest` | 3 | **PASS** | Sliding-window IP limits on auth (10/min), payments (15/min), HTTP 429 response with `Retry-After: 60`. |
| **Authentication & Users** | `AuthControllerTest` | 5 | **PASS** | Customer registration, login, JWT token emission, duplicate email rejection (HTTP 400). |
| **Authentication Service** | `AuthServiceTest` | 4 | **PASS** | Password hashing (`BCrypt`), role mapping (`ROLE_CUSTOMER`), profile retrieval. |
| **Payments Gateway** | `PaymentControllerTest` | 2 | **PASS** | Razorpay order creation for verified orders, payment verification signature validation. |
| **Payments Lifecycle** | `PaymentServiceTest` | 5 | **PASS** | IDOR protection (calling user vs order owner), automated refund processing on cancellation, webhook capture. |
| **Order State Machine** | `OrderStatusTransitionValidatorTest` | 4 | **PASS** | Valid transitions (`PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `OUT_FOR_DELIVERY` $\rightarrow$ `DELIVERED`), rejection of illegal skips (`PENDING` $\rightarrow$ `DELIVERED`). |
| **WhatsApp Outbox** | `WhatsAppNotificationServiceTest` | 3 | **PASS** | Customer status updates, notification deduplication, bounded exponential retry worker. |
| **WhatsApp Webhook** | `WhatsAppWebhookControllerTest` | 3 | **PASS** | Meta challenge verification (`hub.challenge`), HMAC SHA-256 webhook event parsing. |
| **WhatsApp Actions** | `WhatsAppWebhookServiceTest` | 7 | **PASS** | Maker phone verification, interactive button transitions, idempotency deduplication of duplicate messages. |
| **Reservations & Booking** | `ReservationControllerTest` | 2 | **PASS** | Table booking creation (HTTP 201), anonymous user protection (HTTP 403 Forbidden). |
| **Customer Reviews** | `ReviewControllerTest` | 2 | **PASS** | Restaurant-level reviews (`POST /api/reviews/restaurant`), anonymous protection. |
| **Public Inquiries** | `ContactControllerTest` | 3 | **PASS** | Valid contact submission (HTTP 200), validation of required fields (HTTP 400). |
| **Cart Operations** | `CartServiceTest` | 4 | **PASS** | Add items, calculate subtotals, update quantity bounds (1–20), clear cart. |
| **Admin Image Storage** | `AdminMenuImageControllerTest` | 8 | **PASS** | 5MB file upload limit, MIME type whitelist (JPEG/PNG/WebP), orphaned file cleanup on deletion. |
| **Context Bootstrap** | `KaveriApplicationTests` | 1 | **PASS** | Spring application context loads cleanly with all bean dependencies wired. |

**Total Automated Coverage**: **56 Tests / 0 Failures / 0 Errors**
