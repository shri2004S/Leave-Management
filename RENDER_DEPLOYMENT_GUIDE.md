# 🚀 Render & Neon PostgreSQL Deployment Guide - LeaveHub

This comprehensive guide walks you step-by-step through setting up **Neon Serverless PostgreSQL** and deploying the full-stack **Employee Leave Management System (LeaveHub)** to **Render**.

---

## 🏗️ Architecture Overview

The application is deployed as a **Unified Multi-Stage Docker Container**:
1. **Stage 1 (Frontend):** Node 20 builds the React Vite application into optimized static assets.
2. **Stage 2 (Backend):** Maven packages Spring Boot with the built frontend embedded in `src/main/resources/static`.
3. **Stage 3 (Runtime):** Eclipse Temurin OpenJDK 17 lightweight JRE runs the unified app on Render.

### Why this setup is optimal for Render + Neon:
- ✅ **100% Free-Tier Friendly:** Requires only **one** Render Web Service ($0/month) instead of two.
- ✅ **Zero CORS Issues:** Frontend and backend share the exact same origin (`https://<your-app>.onrender.com`).
- ✅ **Secure Cookies:** JWT HttpOnly cookies work seamlessly without third-party cookie restrictions.
- ✅ **Automatic Database Setup:** Spring Boot Hibernate automatically creates all tables and seeds the admin/employee accounts into Neon on the first startup.

---

## 🐘 Step 1: Create Your Neon PostgreSQL Database

If you want to create a brand-new Neon database from scratch:

1. Go to [https://console.neon.tech](https://console.neon.tech) and sign up / log in (you can use your GitHub or Google account).
2. Click **Create Project**:
   - **Project Name:** `leavehub-db` (or any name)
   - **Postgres Version:** `16` or `17` (default)
   - **Region:** Choose a region close to your Render deployment (e.g., `AWS US East (Ohio)` or `AWS Asia Pacific (Singapore)`).
3. Once created, Neon displays the **Connection Details** dashboard:
   - Select **Connection string** dropdown.
   - Switch mode from **Pooled connection** to **Direct connection** (or Direct Host).
   - Your connection string will look like:
     ```text
     postgresql://neondb_owner:YOUR_PASSWORD@ep-sample-host.region.aws.neon.tech/neondb?sslmode=require
     ```
4. Extract your connection details:
   - **Host:** `ep-sample-host.region.aws.neon.tech`
   - **Database Name:** `neondb`
   - **Username:** `neondb_owner`
   - **Password:** `YOUR_PASSWORD`
   - **JDBC URL format for Spring Boot:**
     ```text
     jdbc:postgresql://ep-sample-host.region.aws.neon.tech/neondb?sslmode=require
     ```

> [!NOTE]
> **Existing Neon Database In Project:**
> If you wish to use the pre-configured project Neon database, its credentials are in your `.env` file:
> - **JDBC URL:** `jdbc:postgresql://ep-divine-truth-bizs83uu.c-5.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
> - **Username:** `neondb_owner`
> - **Password:** `npg_vTadwN5YgSr6`

---

## 🚀 Step 2: Deploy to Render

### Method 1: Render Dashboard (Recommended & Simple)

1. Log in to [Render Dashboard](https://dashboard.render.com).
2. Click **New +** (top right) → Select **Web Service**.
3. Under **Connect a repository**, choose your GitHub repository containing the Leave Management project.
4. Fill in the **Service Configuration**:
   | Field | Setting |
   |---|---|
   | **Name** | `leavehub-app` (or any custom name) |
   | **Region** | Oregon (US West) or Ohio |
   | **Branch** | `main` |
   | **Runtime** | **Docker** |
   | **Dockerfile Path** | `./Dockerfile` |
   | **Instance Type** | **Free** ($0 / month) |

5. Scroll down to **Environment Variables** and add the following keys:

   | Key | Value | Description |
   |---|---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<YOUR_NEON_HOST>/neondb?sslmode=require` | Your Neon PostgreSQL JDBC URL |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` | Neon database username |
   | `SPRING_DATASOURCE_PASSWORD` | `<YOUR_NEON_PASSWORD>` | Neon database password |
   | `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 256-bit secret key for signing tokens |
   | `APP_COOKIE_SECURE` | `true` | Enforces HTTPS-only secure cookies on Render |
   | `JAVA_OPTS` | `-Xms256m -Xmx480m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError` | Prevents Out-Of-Memory on Render 512MB RAM |

6. Click **Create Web Service**.

Render will now:
- Clone your repository.
- Run Stage 1: Build the React SPA with Vite.
- Run Stage 2: Embed the frontend inside Spring Boot static assets and package `app.jar`.
- Run Stage 3: Launch OpenJDK 17 and start Tomcat on Render's dynamic `$PORT`.

---

### Method 2: 1-Click Blueprint (Using `render.yaml`)

1. In Render Dashboard, click **New +** → **Blueprint**.
2. Select your repository.
3. Render automatically parses [render.yaml](file:///c:/Users/shri/OneDrive/Desktop/Empolyee Leave Management New - Settribe/render.yaml).
4. Enter your `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` when prompted.
5. Click **Apply**.

---

## 🔑 Step 3: Default Seed Accounts

Upon successful connection to Neon PostgreSQL, Spring Boot's [DataInitializer.java](file:///c:/Users/shri/OneDrive/Desktop/Empolyee Leave Management New - Settribe/backend/src/main/java/LMS/LeaveManagement/config/DataInitializer.java) automatically seeds the database with the following initial accounts:

### 👑 Administrator Accounts
| Name | Email | Password | Role |
|---|---|---|---|
| **Shri (Admin)** | `shri1@gmail.com` | `Password@123` | `ADMIN` |
| **Manideep Sir** | `manideep@gmail.com` | `Password@123` | `ADMIN` |
| **Sushil Sir** | `sushil@gmail.com` | `Password@123` | `ADMIN` |
| **Gaurav Sir** | `gaurav@gmail.com` | `Password@123` | `ADMIN` |

### 👤 Test Employee Account
| Name | Email | Password | Role |
|---|---|---|---|
| **John Doe** | `john@settribe.com` | `Password@123` | `EMPLOYEE` |

---

## 🩺 Step 4: Verification & Health Checks

Once your Render deployment shows **Live**:

1. Open your browser to your live Render URL:
   ```
   https://<your-service-name>.onrender.com
   ```
2. Check the backend health:
   ```
   https://<your-service-name>.onrender.com/api/health
   ```
   *Expected response:* `{"status": "UP"}`
3. Check the database connection to Neon:
   ```
   https://<your-service-name>.onrender.com/api/health/db
   ```
   *Expected response:* `{"status":"UP","database":"PostgreSQL","databaseVersion":"18.7...","connectionValid":true}`
4. Sign in with `shri1@gmail.com` and `Password@123` to access the Admin Dashboard.

---

## 💡 Troubleshooting & Render Free Tier Tips

| Issue | Cause | Solution |
|---|---|---|
| **Cold start delay (~30–50s)** | Render free tier puts web services to sleep after 15 minutes of inactivity. | Allow ~30–50 seconds on the first load for the JVM container to wake up, or set up a free uptime monitor (e.g. UptimeRobot or Cron-Job.org) pinging `https://<your-app>.onrender.com/api/health` every 10 minutes. |
| **Neon database sleep / cold start** | Neon serverless database scales down to zero when idle. | Spring Boot's Hikari connection pool automatically reconnects with its 20s connection timeout. |
| **Out of Memory (OOM / exit code 137)** | Java heap exceeding Render Free Tier 512MB RAM limit. | The `JAVA_OPTS` setting (`-Xms256m -Xmx480m -XX:+UseG1GC`) configured in `render.yaml` and `Dockerfile` ensures memory stays strictly within bounds. |
| **Cookies not persisting across pages** | HTTP vs HTTPS mismatch. | Ensure `APP_COOKIE_SECURE=true` is set on Render (Render serves all traffic over HTTPS). |
