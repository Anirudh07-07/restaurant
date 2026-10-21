# Kaveri Restaurant — Production & Commercial Readiness Evaluation

**Audit Date**: October 21, 2026  
**Auditor**: Senior Backend Architect & Production Reviewer  
**Status**: **Technically Deployable & Live** (Commercialization Roadmap Defined)

---

## 1. Commercial Readiness Checklist

| Evaluation Area | Status | Evidence & Verified Findings |
| :--- | :---: | :--- |
| **Production Build & Packaging** | **PASS** | Multi-stage Docker container builds cleanly (`eclipse-temurin:21-jre-alpine`), packaged as non-root user `foodnest:foodnest`. |
| **Automated Regression Suite** | **PASS** | 56 tests across 14 test suites running on JUnit 5 with zero failures. |
| **Database Persistence** | **PASS** | Managed PostgreSQL 18 instance running on Render with automated table creation and relational constraints. |
| **Zero Code Changes for Config** | **PASS** | 100% of external secrets, payment keys, WhatsApp tokens, database credentials, and ports are read from environment variables. |
| **API Rate Limiting & DoS Shield** | **PASS** | In-memory sliding window rate limiter protects `/api/auth/**` (10/min) and `/api/payments/**` (15/min). |
| **Strict Security Headers** | **PASS** | CSP, HSTS (`max-age=31536000`), `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff` active on all HTTP responses. |
| **Payment Gateway Sandbox Mock** | **PASS** | Razorpay mock mode allows live demonstrations, order placement, and automatic refunds without charging real credit cards. |
| **WhatsApp Two-Way Sync** | **PASS** | Transactional outbox with exponential retry + webhook idempotency table prevents duplicate messages. |
| **Health Monitoring** | **PASS** | Spring Boot Actuator exposes `/actuator/health` and `/actuator/info` (live status: `{"status":"UP"}`). |
| **Automated Offsite DB Backups** | **NOT VERIFIED** | Render provides automatic daily backups on paid tiers; manual SQL dumps required on free tier. |
| **Real Razorpay Live Credentials** | **NOT VERIFIED** | Requires restaurant owner's verified Razorpay merchant account and bank details. |
| **Meta WhatsApp Business Approval** | **NOT VERIFIED** | Requires Meta Business Verification and template approval for customer-initiated messaging windows. |

---

## 2. Distinction: Deployable vs. Sale-Ready

### What is 100% Ready Today:
1. **Live Demonstration Product**: Can be shown to prospective restaurant clients, investors, or franchise partners immediately on `https://kaveri-restaurant.onrender.com`.
2. **Interactive Ordering & Kitchen Flow**: Orders placed via Web or Simulated WhatsApp trigger order notifications, kitchen assignment, status updates, and customer updates.
3. **Database Integrity**: Relational constraints, foreign keys, pluralized table names, optimistic locking (`@Version`), and cascade behaviors verified.
4. **Clean Code & Architecture**: Clean separation between Controllers, Services, Entities, DTOs, and JPA Repositories.

### What is Required Before Commercial Handoff to a Paying Client:
1. **Production Merchant Credentials**:
   - Client must provide their own Razorpay Key ID, Key Secret, and Webhook Secret from [Razorpay Dashboard](https://dashboard.razorpay.com).
   - Once provided, toggle `RAZORPAY_MOCK_ENABLED=false` in Render environment settings.
2. **Meta WhatsApp Business Account**:
   - Client must register their official phone number (e.g. `+91 8235520520`) with the [Meta WhatsApp Business Platform](https://developers.facebook.com).
   - Pre-approve message templates for order notifications (`order_confirmed`, `out_for_delivery`).
   - Toggle `WHATSAPP_MOCK_ENABLED=false`.
3. **Managed Database Plan**:
   - For a commercial production restaurant receiving hundreds of orders daily, upgrade from Render Free PostgreSQL to a managed Starter/Standard plan to ensure automated point-in-time recovery and zero inactivity spin-down.

---

## 3. Operational Costs & External Dependencies

| Component | Provider | Development / Demo Tier | Commercial Production Tier |
| :--- | :--- | :--- | :--- |
| **Compute Engine** | Render (Docker) | Free ($0/mo) | Starter ($7/mo) — no sleep |
| **PostgreSQL Database** | Render Postgres 18 | Free ($0/mo) | Starter ($7/mo) — persistent backups |
| **Payment Gateway** | Razorpay | Test Sandbox ($0) | 2% per domestic transaction |
| **WhatsApp Cloud API** | Meta Graph API | Test Number (Free tier: 1,000 free service convos/mo) | ~₹0.40 - ₹0.80 per utility message |
| **Custom Domain & SSL** | Cloudflare / Namecheap | Cloudflare Free SSL | ~\$10-\$15 / year domain registration |

---

## 4. Disaster Recovery & Rollback Procedure

### Rollback on Deployment Failure:
1. Open [Render Dashboard](https://dashboard.render.com/web/srv-dbc2731srm7s739d64o0/deploys).
2. Click on the last known stable deployment (e.g. commit `3bd1f9c`).
3. Click **Rollback to this deploy**.

### Database Backup & Restore:
```bash
# Manual snapshot backup from Render Postgres
pg_dump -h <host> -U <user> -d kaveridb -F c -b -v -f kaveri_backup_$(date +%Y%m%d).dump

# Restore to emergency fallback database
pg_restore -h <fallback_host> -U <user> -d kaveridb -v kaveri_backup_20261021.dump
```
