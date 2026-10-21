# Nirvaha — Docker & Deployment Guide (Phase 11E)

This document describes the reproducible Docker containerization architecture, multi-stage builds, persistent evidence volume configuration, SPA routing fallback, and deployment instructions for the Nirvaha civic intelligence platform.

---

## 1. Container Architecture Overview

```text
                               ┌────────────────────────────────┐
                               │   React Frontend (Nginx SPA)   │
                               │   Port 80                      │
                               └───────────────┬────────────────┘
                                               │
                                               │ HTTP / REST API
                                               ▼
                               ┌────────────────────────────────┐
                               │  Spring Boot Backend (Java 21) │
                               │  Port 8080 (Non-Root appuser)  │
                               └───────────────┬────────────────┘
                                               │
                       ┌───────────────────────┴───────────────────────┐
                       ▼                                               ▼
        PostgreSQL Database Server                     Persistent Host Storage
  (Neon / Cloud SQL / Host PostgreSQL)                 (Volume: ./data/evidence)
```

---

## 2. Docker Artifacts

### 2.1 Backend Dockerfile (`nirvaha-backend/Dockerfile`)
- **Stage 1 (Builder)**: `maven:3.9.6-eclipse-temurin-21-alpine` compiles `nirvaha-backend` executable JAR using `mvn clean package -DskipTests`.
- **Stage 2 (Runtime)**: `eclipse-temurin:21-jre-alpine` minimal runtime.
- **Security Controls**: Executes as non-root user `appuser` within `appgroup`.
- **Port Exposure**: Exposes port `8080`.
- **Health Probe**: Evaluates `/actuator/health` every 30 seconds (`wget -qO- http://localhost:8080/actuator/health`).

### 2.2 Frontend Dockerfile & SPA Routing (`nirvaha-frontend/Dockerfile` & `nginx.conf`)
- **Stage 1 (Builder)**: `node:20-alpine` runs `npm ci` and `npm run build`.
  - Declares `ARG VITE_API_BASE_URL` and `ENV VITE_API_BASE_URL=$VITE_API_BASE_URL` before executing `npm run build` so Vite embeds the correct API URL into client bundles during build time.
- **Stage 2 (Runtime)**: `nginx:alpine` serves distribution files from `/usr/share/nginx/html`.
- **SPA Fallback**: `nginx.conf` contains `try_files $uri $uri/ /index.html;` to ensure client routes (e.g. `/citizen`, `/government`, `/incidents/:id`) resolve without server 404 errors.

---

## 3. Persistent Evidence Storage Configuration

> [!IMPORTANT]
> Evidence files MUST NOT be stored in an ephemeral container layer.

- The backend container configures `NIRVAHA_EVIDENCE_STORAGE_PATH=/app/uploads/evidence`.
- `docker-compose.yml` maps host storage to container storage:
  ```yaml
  volumes:
    - ./data/evidence:/app/uploads/evidence
  ```
- **Persistence Behavior**: When the backend container restarts or is recreated (`docker restart nirvaha-backend`), all UUID-named evidence files remain fully intact on the host storage and accessible via the API.

---

## 4. Local Deployment Instructions

### 4.1 Prerequisites
- Docker Engine & Docker Compose (v2.0+)
- PostgreSQL Database (Local or Remote, e.g. Neon)

### 4.2 Building & Running with Docker Compose
```bash
# 1. Build and start containers in detached mode
docker compose up -d --build

# 2. Verify container status and health
docker compose ps

# 3. View backend application logs
docker compose logs -f backend

# 4. Stop containers
docker compose down
```

### 4.3 Manual Docker Commands
```bash
# Build Backend Image
docker build -t nirvaha-backend ./nirvaha-backend

# Build Frontend Image (passing build-time API URL)
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080/api -t nirvaha-frontend ./nirvaha-frontend

# Run Backend Container
docker run -d --name nirvaha-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/nirvaha_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=your_password \
  -e JWT_SECRET=your_super_secret_key_minimum_32_bytes_long! \
  -e NIRVAHA_ALLOWED_ORIGINS=http://localhost \
  -e NIRVAHA_EVIDENCE_STORAGE_PATH=/app/uploads/evidence \
  -v ./data/evidence:/app/uploads/evidence \
  nirvaha-backend

# Run Frontend Container
docker run -d --name nirvaha-frontend -p 80:80 nirvaha-frontend
```

---

## 5. Cloud Deployment Target Architecture

### Frontend Static Hosting
- **Recommended Hosts**: Vercel, Netlify, Cloudflare Pages, or AWS S3 + CloudFront.
- **Build Command**: `npm run build`
- **Environment Variable**: `VITE_API_BASE_URL=<deployed-backend-url>/api`

### Backend Container Hosting
- **Recommended Hosts**: Render, Railway, Fly.io, AWS ECS, or Google Cloud Run.
- **Port**: `8080`
- **Health Check Path**: `/actuator/health`
- **Environment Variables**: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `NIRVAHA_ALLOWED_ORIGINS`, `NIRVAHA_EVIDENCE_STORAGE_PATH`.

### Database & Storage
- **Database**: Managed PostgreSQL (e.g., Neon, Cloud SQL, Supabase).
- **Evidence Storage**: Persistent disk volume mount mapped to `NIRVAHA_EVIDENCE_STORAGE_PATH`.
