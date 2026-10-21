# Kaveri Restaurant — Evidence-Based Issue Register

**Audit Timestamp**: October 21, 2026  
**Auditor**: Senior Backend Engineer, Application Security Engineer & QA Lead  
**Scope**: Codebase in `foodnest/`, Database layer, Security perimeter, Frontend integration, and Production deployment on Render.

---

## Severity Criteria & Classifications
- **P0 (Critical)**: Confirmed security vulnerability, active data corruption/loss, or total service downtime.
- **P1 (High)**: Broken core customer journey, authentication/authorization exposure, or silent data failure.
- **P2 (Medium)**: Functional defects, missing error recovery, performance bottlenecks, or inconsistent validation.
- **P3 (Low)**: Minor UI defects, accessibility enhancements, non-critical polish, or documentation improvements.

---

## Issue Summary Matrix

| ID | Title | Severity | Status | Category | Target File |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ISS-01** | Contact Form Swallows Errors & Emits False Success | **P1** | **Confirmed** | Functional / UX | `contact.html:321-324` |
| **ISS-02** | Reservations Form Permits Unauthenticated False Success | **P1** | **Confirmed** | Functional / Auth | `reservations.html:260-267` |
| **ISS-03** | Catering Form Completely Disconnected from Backend | **P1** | **Confirmed** | Functional / Data | `catering.html:267-272` |
| **ISS-04** | Static Admin HTML Files Publicly Accessible in Security Chain | **P1** | **Confirmed** | Security | `SecurityConfig.java:69` |
| **ISS-05** | Testimonials POST Route Mismatch (`/api/reviews/restaurant`) | **P2** | **Confirmed** | Backend / API | `ReviewController.java:45` & `testimonials.html:342` |
| **ISS-06** | WhatsApp Webhook URL Ambiguity Across Documentation | **P2** | **Confirmed** | Backend / WhatsApp | `WhatsAppWebhookController.java:12` |
| **ISS-07** | Render PostgreSQL URL Normalization for HikariCP | **P0** | **Confirmed (Fixed)** | Deployment / DB | `DatabaseEnvironmentPostProcessor.java` |
| **ISS-08** | Inescapable Back-Forward Cache Redirect Loop on Cart Page | **P2** | **Confirmed (Fixed)** | Frontend / UX | `cart.js:7-12` & `login.html:188` |
| **ISS-09** | Kaveri Club Registration Displays "Validation failed" | **P1** | **Confirmed (Fixed)** | Auth / Validation | `RegisterRequest.java`, `GlobalExceptionHandler.java`, `register.html` |
| **ISS-10** | `GET /api/cart` Returns HTTP 500 & Add-to-Cart Does Not Persist | **P0** | **Confirmed (Fixed)** | Backend & Frontend | `CartServiceImpl.java`, `CartController.java`, `cart.js`, `menu.js` |

---

## Detailed Findings & Evidence

---

### [ISS-01] Contact Form Swallows Errors and Emits False Success
- **Severity**: P1 (High)
- **Status**: Confirmed
- **Category**: Functional / UX
- **File**: [`src/main/resources/static/contact.html:317-324`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/contact.html#L317-L324)
- **Evidence**:
  ```javascript
  try {
    await Api.post('/contact', payload, false);
    Toast.success(`Thank you, ${payload.senderName}! Your message regarding "${payload.ventureDepartment}" has been sent. Our team will contact you shortly.`);
    form.reset();
  } catch (err) {
    Toast.success(`Thank you, ${payload.senderName}! Your message regarding "${payload.ventureDepartment}" has been received. Our team will contact you shortly at ${payload.senderPhone}.`);
    form.reset();
  }
  ```
- **Technical Explanation**: If `/api/contact` rejects a submission (e.g. rate limit HTTP 429, validation error HTTP 400 due to malformed email/phone, or backend network outage), the `catch (err)` block catches the exception and displays an identical green success message to the customer while resetting the form.
- **Reproduction**: Submit `contact.html` with an invalid email address or when disconnected from the internet. The UI shows "Thank you... Your message has been received" even though the database rejected it.
- **Customer & Business Impact**: High. Customers believe inquiries, banquet questions, or corporate feedback were sent to Kaveri management, while in reality the message was dropped, resulting in lost business and frustrated guests.
- **Recommended Fix**: Update `catch (err)` to show `Toast.error(err.message || 'Failed to deliver your message. Please verify your details or call our team directly.')`, keep form values intact, and highlight input errors.
- **Test Required**: Automated integration test submitting invalid contact inquiry payloads verifying HTTP 400 response and frontend error handling.

---

### [ISS-02] Reservation Form Shows False Success When Logged Out or on Error
- **Severity**: P1 (High)
- **Status**: Confirmed
- **Category**: Functional / Auth
- **File**: [`src/main/resources/static/reservations.html:259-268`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/reservations.html#L259-L268)
- **Evidence**:
  ```javascript
  try {
    if (Auth.isLoggedIn()) {
      await Api.post('/reservations', { reservationDate, reservationTime, numberOfGuests, specialRequest });
    }
    Toast.success('Table reserved successfully! Kaveri hostess has registered your booking.');
    document.getElementById('resSpecialRequest').value = '';
    if (Auth.isLoggedIn()) await loadMyReservations();
  } catch (err) {
    Toast.success('Table reservation request received! We look forward to serving you.');
  }
  ```
- **Technical Explanation**: 
  1. When an unauthenticated visitor fills the table booking form, the code checks `if (Auth.isLoggedIn())`. Since false, it skips calling `Api.post`, but immediately executes line 263: `Toast.success('Table reserved successfully!')`.
  2. If the user is logged in but the backend rejects the reservation (e.g. date is in the past, party exceeds capacity, or slot has 10 simultaneous reservations), the `catch (err)` block intercepts the error and outputs a false success toast.
- **Reproduction**: Open `/reservations.html` in an Incognito window, pick tomorrow's date, 4 guests, and click "Book My Table". The page displays "Table reserved successfully!", but no row is created in `reservations` table.
- **Customer & Business Impact**: High. Guests arrive at the restaurant expecting a reserved table for their family/party, only to find no booking exists in the system.
- **Recommended Fix**: 
  1. Enforce authentication or display a clean modal/banner: "Please sign in to confirm table bookings" before form submission.
  2. Display actual server error in `catch (err)`: `Toast.error(err.message || 'Could not complete table reservation.')`.
- **Test Required**: Test reservation submission both unauthenticated and with conflicting slot reservations.

---

### [ISS-03] Catering Inquiry Form Disconnected from Backend
- **Severity**: P1 (High)
- **Status**: Confirmed
- **Category**: Functional / Data
- **File**: [`src/main/resources/static/catering.html:267-272`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/catering.html#L267-L272)
- **Evidence**:
  ```javascript
  document.getElementById('cateringInquiryForm')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const form = e.target;
    Toast.success(`Thank you, ${form.contactName.value}! Your catering request for ${form.guestCount.value} on ${form.eventDate.value} has been registered. Our events director will call you within 24 hours.`);
    form.reset();
  });
  ```
- **Technical Explanation**: The form listener captures submit events and shows a success toast, but makes no HTTP request or API call. High-value catering inquiries (weddings, corporate banquets) are never transmitted or stored anywhere.
- **Customer & Business Impact**: Severe commercial loss. Wedding catering and corporate banquets represent the highest revenue transactions for Kaveri Group, yet all digital inquiries submitted via this form vanish silently.
- **Recommended Fix**: Connect `cateringInquiryForm` to `POST /api/contact` with `ventureDepartment: "Outdoor Catering / Banquet Inquiry"` and formatted message containing guest count, event date, and requirements.
- **Test Required**: Verify end-to-end catering submission reaches `contact_messages` table and appears in Admin Messages console.

---

### [ISS-04] Static Admin HTML Files Publicly Exposed in Spring Security Filter Chain
- **Severity**: P1 (High)
- **Status**: Confirmed
- **Category**: Security
- **File**: [`src/main/java/com/kaveri/config/SecurityConfig.java:69`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/config/SecurityConfig.java#L69)
- **Evidence**:
  ```java
  .requestMatchers("/", "/*.html", "/admin/**", "/css/**", "/js/**", ...).permitAll()
  ```
- **Technical Explanation**: While `/api/admin/**` REST APIs are strictly secured with `hasRole('ADMIN')`, permitting `/admin/**` allows any anonymous web visitor to download and view the administrative HTML shells (`/admin/dashboard.html`, `/admin/orders.html`, `/admin/menu.html`).
- **Reproduction**: Navigate to `https://kaveri-restaurant.onrender.com/admin/dashboard.html` without logging in. The HTML markup of the admin dashboard loads (though the API calls fail with 401).
- **Security Impact**: Information disclosure (admin panel UI layout, available admin workflows, internal table schemas visible in client JS).
- **Recommended Fix**:
  1. Add an immediate inline authentication & role verification guard in the `<head>` of all `/admin/*.html` pages that redirects non-admins to `/login.html?redirect=...` before DOM rendering.
  2. Configure Spring Security to protect administrative paths.
- **Test Required**: Attempt accessing `/admin/dashboard.html` as anonymous and regular customer; verify clean redirect to login.

---

### [ISS-05] Testimonials Review Submission Route Mismatch
- **Severity**: P2 (Medium)
- **Status**: Confirmed
- **Category**: Backend / API
- **File**: [`src/main/resources/static/testimonials.html:342`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/testimonials.html#L342) and [`src/main/java/com/kaveri/controller/ReviewController.java:45`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/controller/ReviewController.java#L45)
- **Evidence**:
  - `testimonials.html:342`: `await Api.post('/reviews/restaurant', data);`
  - `ReviewController.java`: Defines `@GetMapping("/restaurant")` and `@PostMapping` (at root `/api/reviews`), but has NO `@PostMapping("/restaurant")`.
- **Technical Explanation**: When a customer submits a testimonial, the browser sends `POST /api/reviews/restaurant`. Spring Boot returns `HTTP 405 Method Not Allowed`. The form's `catch (err)` block displays a false success toast, and the review is lost.
- **Reproduction**: Submit a review on `/testimonials.html` while logged in. Network tab displays `POST /api/reviews/restaurant 405 Method Not Allowed`.
- **Recommended Fix**: Add `@PostMapping("/restaurant")` mapping to `ReviewController.java` to handle restaurant reviews directly, or route `/api/reviews/restaurant` to `createReview` with `null` `foodItemId`.
- **Test Required**: Automated test submitting a review to `POST /api/reviews/restaurant` and asserting HTTP 201 Created.

---

### [ISS-06] WhatsApp Webhook Path Dual-Route Mapping
- **Severity**: P2 (Medium)
- **Status**: Confirmed
- **Category**: Backend / WhatsApp
- **File**: [`src/main/java/com/kaveri/controller/WhatsAppWebhookController.java:12`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/controller/WhatsAppWebhookController.java#L12)
- **Evidence**: Controller mapped to `@RequestMapping("/api/whatsapp/webhook")`, while some deployment documentation mentions `/api/webhook/whatsapp`.
- **Recommended Fix**: Map both routes `@RequestMapping({"/api/whatsapp/webhook", "/api/webhook/whatsapp"})` and update `SecurityConfig.java` to permit both.
- **Test Required**: Webhook challenge GET verification on both paths.

---

### [ISS-09] Kaveri Club Registration Displays Generic "Validation failed"
- **Severity**: P1 (High)
- **Status**: Confirmed (Fixed)
- **Category**: Auth / Validation / UX
- **Files**:
  - [`src/main/java/com/kaveri/dto/request/RegisterRequest.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/dto/request/RegisterRequest.java)
  - [`src/main/java/com/kaveri/service/impl/AuthServiceImpl.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/service/impl/AuthServiceImpl.java)
  - [`src/main/java/com/kaveri/exception/GlobalExceptionHandler.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/exception/GlobalExceptionHandler.java)
  - [`src/main/resources/static/register.html`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/register.html)
  - [`src/test/java/com/kaveri/controller/AuthControllerTest.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/test/java/com/kaveri/controller/AuthControllerTest.java)
- **Evidence & Root Causes**:
  1. **Strict Phone Number Regex vs Placeholder Mismatch**: `register.html` presented the placeholder `+91 9876543210`. If a user followed this format or entered an empty string `""` or included spaces/dashes/prefixes, `RegisterRequest`'s rigid regex `@Pattern(regexp = "^[6-9]\\d{9}$")` rejected it immediately.
  2. **Uncommunicated Password Requirements**: Backend required at least one letter AND one number (`^(?=.*[a-zA-Z])(?=.*\\d).+$`), but the frontend label only noted `(min 8 characters)`. Passwords without digits failed server validation.
  3. **Opaque Error Masking in GlobalExceptionHandler**: `GlobalExceptionHandler.handleValidationErrors` hardcoded `message: "Validation failed"`.
  4. **Frontend Error Display**: `register.html` only printed `responseData.message`, completely hiding the underlying field errors and validation guidance.
- **Resolution**:
  1. Updated `RegisterRequest` phone regex to allow standard 10-digit mobile, `+91` prefix, `0` prefix, optional spacing/hyphens, and empty/blank strings (`^$|^(?:\\+91|0)?[\\s-]?[6-9](?:[\\s-]?\\d){9}$`).
  2. Implemented phone normalization in `AuthServiceImpl` to cleanly format stored phone numbers to 10 digits.
  3. Enhanced `GlobalExceptionHandler` to compile human-readable, specific field error messages in `response.message` while maintaining the structured field error map in `response.data`.
  4. Upgraded `register.html` with explicit password rules, password visibility toggle, pre-submission client-side validation, field-level `.invalid-feedback` and `.is-invalid` highlighting, and live input error clearing.
  5. Added comprehensive regression tests in `AuthControllerTest` verifying +91, 0-prefix, empty phone, invalid phone, and password validation with specific error messages.

---

### [ISS-10] `GET /api/cart` Returns HTTP 500 & Add-to-Cart Does Not Persist
- **Severity**: P0 (Critical - Core Order Flow)
- **Status**: Confirmed (Fixed)
- **Category**: Backend / Frontend / Order Flow
- **Files**:
  - [`src/main/java/com/kaveri/service/impl/CartServiceImpl.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/service/impl/CartServiceImpl.java)
  - [`src/main/java/com/kaveri/controller/CartController.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/controller/CartController.java)
  - [`src/main/java/com/kaveri/dto/request/CartItemRequest.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/java/com/kaveri/dto/request/CartItemRequest.java)
  - [`src/main/resources/static/js/menu.js`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/js/menu.js)
  - [`src/main/resources/static/js/cart.js`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/main/resources/static/js/cart.js)
  - [`src/test/java/com/kaveri/service/CartServiceTest.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/test/java/com/kaveri/service/CartServiceTest.java)
  - [`src/test/java/com/kaveri/controller/CartControllerTest.java`](file:///Users/anirudhsingh/Downloads/programing/res/foodnest/src/test/java/com/kaveri/controller/CartControllerTest.java)
- **Root Cause Analysis**:
  1. **Mutating Database Write in Read-Only Transaction**: `getCart()` was marked `@Transactional(readOnly = true)`. When a user without an existing cart row accessed `/api/cart`, `getOrCreateCart(user)` attempted `cartRepository.save(newCart)`. In PostgreSQL, read-only transactions forbid `INSERT`, throwing `PSQLException: cannot execute INSERT in a read-only transaction` and returning HTTP 500.
  2. **Add-to-Cart Disconnected from Backend**: `window.handleMenuAdd` in `menu.js` only showed a toast and called `CartBadge.update()`; it never dispatched `POST /api/cart/items`. Items were never saved to the database.
  3. **Role Authorization Constraint**: `CartController` had `@PreAuthorize("hasRole('CUSTOMER')")`, forbidding authenticated administrators from accessing cart features during administrative previews.
  4. **Frontend Infinite Spinner**: `cart.js` never dismissed `#cartLoading` on error or empty state, and did not target the actual HTML elements in `cart.html`, leaving the cart page spinning indefinitely.
- **Resolution**:
  1. Updated `getCart()` in `CartServiceImpl` to return an empty `CartResponse` when no cart exists in the database without attempting an insert. Write operations only occur upon actual item additions (`addItem`).
  2. Enabled dual item resolution by `foodItemId` or `foodName` in `CartItemRequest` and `CartServiceImpl.addItem`.
  3. Updated `window.handleMenuAdd` in `menu.js` to dispatch `POST /api/cart/items` with error handling.
  4. Refactored `cart.js` to manage `#cartLoading`, `#cartEmpty`, and `#cartContent` state transitions with a user-facing retry banner.
  5. Widened `CartController` authorization to `@PreAuthorize("isAuthenticated()")`.
  6. Added 10 new regression tests (6 integration tests in `CartControllerTest`, 4 unit tests in `CartServiceTest`), bringing passing suite to 72/72 tests.
