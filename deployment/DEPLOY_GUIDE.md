# 🚀 Turnkey Deployment Guide — Kaveri Restaurant / FoodNest

This project is fully containerized, production-hardened, and prepared for zero-friction cloud deployment.

---

## 🌟 Method 1: All-In-One Cloud Deploy on Render (Recommended)

Render provides free hosting, automated SSL certificates, automatic container building from Git, and managed databases.

### 1-Click Blueprint Deployment (Using `render.yaml`):
1. Push your repository to **GitHub**:
   ```bash
   git add .
   git commit -m "Production deployment setup"
   git push origin main
   ```
2. Open **[dashboard.render.com](https://dashboard.render.com)**.
3. Click **New +** -> **Blueprint**.
4. Connect your GitHub repository.
5. Render will automatically read [`render.yaml`](../render.yaml) and configure:
   - **Database**: PostgreSQL (automatically linked to your app)
   - **Web Service**: Built via [`Dockerfile`](../Dockerfile)
   - **Healthcheck**: `/actuator/health`
   - **Secrets & Profiles**: Auto-configured
6. Click **Apply**. Within 3 minutes, your application will be live at `https://kaveri-restaurant.onrender.com`.

---

## ⚡ Method 2: Deploying on Railway

Railway supports instant Docker deployment and zero-configuration MySQL databases.

1. Install the Railway CLI or visit **[railway.app](https://railway.app)**.
2. Click **New Project** -> **Deploy from GitHub repo**.
3. Click **Add Service** -> **Database** -> **Add MySQL**.
4. Railway will automatically link the MySQL service to your container.
5. Add any custom environment variables (e.g., `RAZORPAY_KEY_ID`, `WHATSAPP_API_TOKEN`) under the **Variables** tab.
6. Click **Generate Domain** to get a public HTTPS address (e.g., `https://kaveri.up.railway.app`).

---

## 🌐 Method 3: Hybrid Deployment (Frontend on Vercel + Backend on Render/Railway)

If you want your website hosted directly on **Vercel** (`https://your-restaurant.vercel.app`):

### Step 1: Deploy Backend
Deploy your backend first using **Method 1 (Render)** or **Method 2 (Railway)** to obtain your live backend URL (e.g. `https://kaveri-restaurant.onrender.com`).

### Step 2: Deploy Frontend to Vercel
1. Open [`src/main/resources/static/vercel.json`](../src/main/resources/static/vercel.json).
2. Update the `destination` URL to your live backend domain:
   ```json
   {
     "version": 2,
     "cleanUrls": true,
     "trailingSlash": false,
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://kaveri-restaurant.onrender.com/api/:path*"
       }
     ]
   }
   ```
3. Deploy to Vercel:
   ```bash
   cd src/main/resources/static
   npx vercel --prod
   ```
   Or push to GitHub and import the `src/main/resources/static` directory in the **Vercel Web Dashboard** (Root Directory: `foodnest/src/main/resources/static`).

*Vercel's edge network will now serve all HTML, CSS, images, and JavaScript with global CDN caching, while seamlessly proxying all API requests (`/api/*`) to your backend without any CORS issues!*

---

## 🖥️ Method 4: Self-Hosted Cloud VM / VPS (AWS EC2, DigitalOcean, Hetzner, Ubuntu)

If deploying to your own Linux server:

1. Clone repository to server:
   ```bash
   git clone <your-repo-url>
   cd res/foodnest
   ```
2. Create your `.env` configuration:
   ```bash
   cp .env.example .env
   # Edit .env with your domain, secrets, and credentials
   nano .env
   ```
3. Start the production cluster:
   ```bash
   docker compose up -d --build
   ```
4. Verify health:
   ```bash
   curl http://localhost:8080/actuator/health
   # Expected response: {"status":"UP"}
   ```
5. Configure Nginx reverse proxy using the pre-configured template in [`deployment/nginx.conf`](./nginx.conf).

---

## 📋 Production Environment Variables Reference

| Variable | Recommended Production Value | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production configuration |
| `DATABASE_URL` | `jdbc:mysql://...` or `jdbc:postgresql://...` | Cloud database connection string |
| `DATABASE_USERNAME` | `<username>` | Database user |
| `DATABASE_PASSWORD` | `<password>` | Database password |
| `JWT_SECRET` | 32+ character random string | Token signing secret |
| `RAZORPAY_KEY_ID` | `rzp_live_...` | Razorpay Merchant Key ID |
| `RAZORPAY_KEY_SECRET` | `...` | Razorpay Secret |
| `RAZORPAY_WEBHOOK_SECRET` | `...` | Webhook verification secret |
| `RAZORPAY_MOCK_ENABLED` | `false` | Set to `true` for simulated test payments |
| `WHATSAPP_API_TOKEN` | `EAA...` | Meta System User token |
| `WHATSAPP_PHONE_NUMBER_ID`| `<phone_number_id>` | WhatsApp Cloud API phone ID |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | `kaveri_whatsapp_verify_token_est1947` | Meta webhook verify token |
| `WHATSAPP_APP_SECRET` | `<app_secret>` | Meta App Secret for HMAC validation |
| `WHATSAPP_RESTAURANT_PHONE` | `918235520520` | Kitchen maker WhatsApp number |
| `WHATSAPP_MOCK_ENABLED` | `false` | Set to `true` for simulated WhatsApp messaging |
| `RATELIMIT_ENABLED` | `true` | Enables brute-force and DDoS rate limiting |
