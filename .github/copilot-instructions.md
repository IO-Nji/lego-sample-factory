# AI Coding Agent Guide

This repo implements LIFE (LEGO Integrated Factory Execution): a Dockerized microservice backend with a React + Vite frontend, fronted by an nginx root proxy. Use these instructions to move quickly and safely in this codebase.

## Architecture Overview
- Entry point: nginx root proxy serving the frontend and forwarding `/api/*` to the API Gateway. See [nginx-root-proxy/nginx.conf](nginx-root-proxy/nginx.conf) and [lego-factory-frontend/nginx.conf](lego-factory-frontend/nginx.conf).
- Frontend: React 18 + Vite, containerized with nginx. Key app code in [lego-factory-frontend/src](lego-factory-frontend/src). API calls go through `/api/*` with Axios. See [lego-factory-frontend/src/api/api.js](lego-factory-frontend/src/api/api.js) and [lego-factory-frontend/src/context/AuthContext.jsx](lego-factory-frontend/src/context/AuthContext.jsx).
- Backend: Spring Boot microservices behind Spring Cloud Gateway. Services: `user-service`, `masterdata-service`, `inventory-service`, `order-processing-service`, `simal-integration-service`. Each has its own H2 database. See service folders under [lego-factory-backend](lego-factory-backend).
- Gateway: Routes all API requests, handles JWT and CORS. See [lego-factory-backend/api-gateway](lego-factory-backend/api-gateway).
- High-level system docs and defaults: [README.md](README.md).

## Developer Workflows
- Docker (recommended):
  - Start all services: `docker-compose up -d` from [lego-sample-factory](.). Or run [start-factory.ps1](start-factory.ps1) on Windows.
  - Stop: `docker-compose down` (add `-v` to reset H2 databases).
  - Logs: `docker-compose logs -f [service]` (e.g., `api-gateway`, `user-service`, `frontend`).
  - Rebuild service after code change: `docker-compose build --no-cache [service] && docker-compose up -d [service]`.
- Local dev (without Docker):
  - Backend: use Maven wrapper in each service directory, e.g. `./mvnw spring-boot:run` under [lego-factory-backend/user-service](lego-factory-backend/user-service).
  - Frontend: in [lego-factory-frontend](lego-factory-frontend): `npm install && npm run dev` (app on `http://localhost:5173`).
  - In local dev, call backend directly on ports 8011–8016 or adjust proxy config.

## Access & Auth
- Default login for testing: `lego_admin` / `password`. See user table in [README.md](README.md).
- JWT-based auth flows via API Gateway; frontend stores token and applies `Authorization: Bearer` headers in Axios. See [lego-factory-frontend/src/context/AuthContext.jsx](lego-factory-frontend/src/context/AuthContext.jsx) and [lego-factory-frontend/src/api/api.js](lego-factory-frontend/src/api/api.js).

## Conventions & Patterns
- Service boundaries: Each Spring service encapsulates its domain (controller → service → repository → entity → dto). Example structure in [lego-factory-backend/order-processing-service/README.md](lego-factory-backend/order-processing-service/README.md).
- Databases: H2 per service for development; reset via `docker-compose down -v`.
- API base paths: Always route through Gateway under `/api/...` when running Docker; local dev may use service ports directly.
- Error handling: Global exception handlers return standardized JSON; expect consistent error shapes across backend services.
- Logging & health: Use Spring Actuator health endpoints; access via gateway when exposed. See details in [README.md](README.md).
- Frontend routing: React Router pages under [lego-factory-frontend/src/pages](lego-factory-frontend/src/pages) with role-based dashboards; protected components enforce auth.

## Key Integration Points
- API Gateway: central routing/auth; ensure new endpoints are exposed via gateway config.
- Inventory ↔ Order Processing: stock updates and fulfillment actions flow through respective services; keep status transitions aligned with documented workflows in [lego-factory-backend/order-processing-service/README.md](lego-factory-backend/order-processing-service/README.md).
- SimAL Integration: scheduling endpoints under the integration service; consumed by production planning UI.

## Typical Tasks for Agents
- Add a frontend page or component: place under [lego-factory-frontend/src/pages](lego-factory-frontend/src/pages) or [lego-factory-frontend/src/components](lego-factory-frontend/src/components), wire via React Router, call APIs using the shared Axios instance in [lego-factory-frontend/src/api/api.js](lego-factory-frontend/src/api/api.js).
- Add a backend endpoint: create controller/service/repository classes in the target service under [lego-factory-backend/<service>/src/main/java](lego-factory-backend), add DTOs as needed, configure route exposure via API Gateway.
- Update role-based access: adjust backend Spring Security and frontend guards in [lego-factory-frontend/src/context/AuthContext.jsx](lego-factory-frontend/src/context/AuthContext.jsx) and protected components.

## Commands & Examples
- Health check via gateway (Docker): `curl http://localhost/api/actuator/health`.
- View container status: `docker-compose ps`; resource usage: `docker stats`.
- Frontend dev server: `npm run dev` from [lego-factory-frontend](lego-factory-frontend).

## Gotchas
- URL base: In Docker, use `/api/...` from the browser; direct service ports are for local dev only.
- H2 state: In-memory or file-backed per service; reset with `docker-compose down -v` if stale data causes issues.
- CORS/JWT: If you add endpoints, verify gateway CORS and JWT validation to avoid 401/403 in the frontend.

## Reference Files
- Top-level docs: [README.md](README.md), config overview: [config_manifest.md](config_manifest.md).
- Backend services: [lego-factory-backend](lego-factory-backend) (each service has its own `Dockerfile`, `pom.xml`, and `src`).
- Frontend app: [lego-factory-frontend](lego-factory-frontend) with Vite config [lego-factory-frontend/vite.config.js](lego-factory-frontend/vite.config.js) and Axios setup [lego-factory-frontend/src/api/api.js](lego-factory-frontend/src/api/api.js).
- Nginx proxy: [nginx-root-proxy/nginx.conf](nginx-root-proxy/nginx.conf).

---
If any sections are unclear or missing (e.g., gateway route specifics or service-by-service endpoint maps), tell me what you need and I’ll refine this guide.