# 🍽️ FoodNest — Restaurant Management & Online Food Ordering System

> **A production-quality, full-stack Restaurant Management System built with Spring Boot 3, Java 21, MySQL, and Bootstrap 5. Designed as a portfolio-grade project showcasing clean architecture, JWT security, payment integration, and a premium UI.**

---

## 🚀 Live Features

### Customer Features
- ✅ Register & Login (JWT Authentication)
- ✅ Browse full menu with beautiful food cards
- ✅ Search food by name/description
- ✅ Filter by category, vegetarian, spicy, price range
- ✅ Sort by price, rating, creation date
- ✅ Paginated results (no full table scans)
- ✅ View detailed food page with reviews
- ✅ Add to cart / Update quantity / Remove items
- ✅ Delivery fee calculation (FREE above ₹500)
- ✅ Tax calculation (5%)
- ✅ Save multiple delivery addresses
- ✅ Set default address
- ✅ Place orders (COD or Online Payment)
- ✅ Razorpay payment integration (server-side signature verification)
- ✅ Order tracking with visual timeline
- ✅ Order history
- ✅ Cancel orders (PLACED/CONFIRMED status)
- ✅ Table reservations with slot management
- ✅ Submit food & restaurant reviews (1-5 stars)
- ✅ Edit & delete own reviews
- ✅ In-app notifications
- ✅ Profile management

### Admin Features
- ✅ Secure admin dashboard with KPI stats
- ✅ Add / Edit / Delete food items
- ✅ Upload food images
- ✅ Toggle food availability
- ✅ Manage food categories (CRUD)
- ✅ View all orders with search & filter
- ✅ Update order status (full lifecycle)
- ✅ View and manage customers (enable/disable)
- ✅ View & manage table reservations
- ✅ Confirm / Reject / Complete reservations
- ✅ Moderate reviews

---

## 🛠️ Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6 + JWT (JJWT 0.12.6) |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Payment | Razorpay Java SDK |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Utilities | Lombok, MapStruct |
| Frontend | HTML5, CSS3, JavaScript (Vanilla), Bootstrap 5 |
| Icons | Font Awesome 6 |
| Fonts | Google Fonts (Playfair Display, Inter) |
| Maps | OpenStreetMap |
| Containers | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test |

---

## 🏗️ Architecture

```
src/main/java/com/foodnest/foodnest/
├── config/           # SecurityConfig, OpenApiConfig, WebConfig, DataSeeder
├── controller/       # REST Controllers (thin, no business logic)
├── service/          # Service interfaces
│   └── impl/         # Service implementations (all business logic here)
├── repository/       # Spring Data JPA repositories
├── entity/           # JPA entities
├── dto/
│   ├── request/      # Incoming request DTOs with validation
│   └── response/     # Outgoing response DTOs (no entity exposure)
├── security/
│   ├── filter/       # JwtAuthenticationFilter
│   └── service/      # JwtService, CustomUserDetailsService
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── enums/            # OrderStatus, PaymentStatus, ReservationStatus, etc.
└── util/             # SecurityUtils
```

**Clean Architecture Principles:**
- No business logic in controllers
- No JPA entities exposed via API
- All prices fetched from DB (never trusted from frontend)
- Ownership checks in service layer

---

## 🗄️ Database Schema

**Tables:** users, roles, user_roles, categories, food_items, carts, cart_items, addresses, orders, order_items, payments, reservations, reviews, notifications

**Key Design Decisions:**
- `order_items` stores food name & price snapshots → historical accuracy preserved
- `orders.delivery_address` stores address snapshot → orders don't break if address deleted
- Indexed: email, food category, order status, reservation date, notification read-status
- BCrypt for all passwords — plain-text NEVER stored

---

## 📡 API Endpoints

### Authentication
```
POST /api/auth/register    Register new customer
POST /api/auth/login       Login and get JWT token
```

### Menu (Public)
```
GET  /api/menu             All food (paginated)
GET  /api/menu/{id}        Food detail
GET  /api/menu/search      Search & filter (keyword, category, price, veg, spicy)
GET  /api/menu/category/{id}  By category
GET  /api/menu/top-rated   Top rated dishes
GET  /api/categories        All categories
```

### Cart (CUSTOMER)
```
GET    /api/cart                Get my cart
POST   /api/cart/items          Add item
PUT    /api/cart/items/{id}     Update quantity
DELETE /api/cart/items/{id}     Remove item
DELETE /api/cart                Clear cart
```

### Orders (CUSTOMER)
```
POST   /api/orders              Place order
GET    /api/orders              My order history
GET    /api/orders/{id}         Order detail
PATCH  /api/orders/{id}/cancel  Cancel order
```

### Payments (CUSTOMER)
```
POST /api/payments/create/{orderId}  Create Razorpay order
POST /api/payments/verify            Verify payment signature
```

### Addresses (CUSTOMER)
```
GET    /api/addresses           My addresses
POST   /api/addresses           Add address
PUT    /api/addresses/{id}      Update address
DELETE /api/addresses/{id}      Delete address
PATCH  /api/addresses/{id}/set-default  Set as default
```

### Reservations (CUSTOMER)
```
POST  /api/reservations         Make reservation
GET   /api/reservations         My reservations
PATCH /api/reservations/{id}/cancel  Cancel
```

### Reviews (Public GET, CUSTOMER POST)
```
GET  /api/reviews/food/{id}     Food reviews (public)
GET  /api/reviews/restaurant    Restaurant reviews (public)
POST /api/reviews               Submit review
PUT  /api/reviews/{id}          Update review
DELETE /api/reviews/{id}        Delete review
```

### Admin (ADMIN only)
```
GET  /api/admin/dashboard
GET  /api/admin/orders?keyword=&status=
PATCH /api/admin/orders/{id}/status?status=
POST  /api/admin/menu
PUT   /api/admin/menu/{id}
DELETE /api/admin/menu/{id}
POST  /api/admin/menu/{id}/image (multipart)
PATCH /api/admin/menu/{id}/toggle-availability
GET/POST/PUT/DELETE /api/admin/categories
GET  /api/admin/users
PATCH /api/admin/users/{id}/toggle-status
GET  /api/admin/reservations
PATCH /api/admin/reservations/{id}/status
GET  /api/admin/reviews
DELETE /api/admin/reviews/{id}
```

---

## ⚙️ Setup & Running Locally

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8.0+
- (Optional) Docker & Docker Compose

### 1. Clone & Configure

```bash
git clone https://github.com/yourusername/foodnest.git
cd foodnest

# Copy env example and fill in values
cp .env.example .env
```

### 2. Create MySQL Database

```sql
CREATE DATABASE foodnest;
```

### 3. Set Environment Variables

```bash
export DATABASE_URL=jdbc:mysql://localhost:3306/foodnest?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=your_password
export JWT_SECRET=your-minimum-32-character-secret-key
export RAZORPAY_KEY_ID=your_razorpay_key_id
export RAZORPAY_KEY_SECRET=your_razorpay_key_secret
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Access at: **http://localhost:8080**

API Docs: **http://localhost:8080/swagger-ui.html**

---

## 🐳 Docker Setup

```bash
# Set required env vars or edit docker-compose.yml
docker compose up --build
```

Access at: **http://localhost:8080**

---

## 🌱 Default Credentials (Seeded on Startup)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@foodnest.com | Admin@1234 |
| Customer | customer@foodnest.com | Customer@1234 |

> **Change these immediately in production!**

---

## 🧪 Running Tests

```bash
mvn test
```

Tests use H2 in-memory database — no MySQL required.

---

## 🔒 Security

- **JWT** stateless authentication
- **BCrypt** password hashing (strength 10)
- Role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`)
- Customers can ONLY access their own resources (enforced at service layer)
- Admin endpoints completely isolated (`/api/admin/**`)
- All prices re-fetched from DB at checkout (frontend price tampering impossible)
- Razorpay HMAC SHA256 signature verified server-side
- CORS configured
- No sensitive data (passwords, JWT secrets) ever returned in API responses

---

## 🌍 Environment Variables Reference

| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | JDBC connection URL | ✅ |
| `DATABASE_USERNAME` | DB username | ✅ |
| `DATABASE_PASSWORD` | DB password | ✅ |
| `JWT_SECRET` | Min 32-char secret key | ✅ |
| `JWT_EXPIRATION` | Token TTL in ms (default: 86400000) | ❌ |
| `RAZORPAY_KEY_ID` | Razorpay public key | ✅ (for online pay) |
| `RAZORPAY_KEY_SECRET` | Razorpay secret key | ✅ (for online pay) |
| `UPLOAD_DIR` | Image upload directory path | ❌ (default: uploads) |

---

## 🗺️ Project Structure

```
foodnest/
├── src/main/java/com/foodnest/foodnest/
├── src/main/resources/
│   ├── static/               # Frontend HTML/CSS/JS
│   │   ├── index.html        # Homepage
│   │   ├── menu.html         # Menu page
│   │   ├── cart.html         # Cart page
│   │   ├── checkout.html     # Checkout
│   │   ├── orders.html       # Order history
│   │   ├── food.html         # Food detail
│   │   ├── reservations.html # Table reservations
│   │   ├── profile.html      # User profile
│   │   ├── login.html        # Login page
│   │   ├── register.html     # Registration
│   │   ├── about.html        # About page
│   │   ├── contact.html      # Contact page
│   │   ├── admin/
│   │   │   └── dashboard.html # Admin panel
│   │   ├── css/style.css     # Main stylesheet
│   │   └── js/main.js        # Shared utilities
│   └── application.properties
├── src/test/java/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
└── pom.xml
```

---

## 🔮 Future Improvements

- [ ] Email notifications (Spring Mail)
- [ ] Push notifications (WebSocket/Firebase)
- [ ] Cloudinary image storage
- [ ] Admin analytics charts (Chart.js)
- [ ] Coupon/discount system
- [ ] Loyalty points
- [ ] Multiple restaurant support
- [ ] Real-time order tracking (WebSocket)
- [ ] Driver assignment system
- [ ] Mobile app (Flutter)
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Rate limiting (Spring RateLimiter)
- [ ] Redis caching for menu

---

## 📄 License

MIT License — free to use for portfolio, educational, and commercial projects.

---

<p align="center">
  <strong>Built with ❤️ using Spring Boot & Bootstrap 5</strong><br>
  <em>FoodNest — Good Food. Great Moments.</em>
</p>
