# LEGO Sample Factory

Containerized microservices demo for a LEGO sample factory, with a React frontend, an API Gateway, and Spring Boot backend services orchestrated via Docker Compose.

## Current Status

- Configuration is driven by a root-level `.env` file consumed by Docker Compose.
- A committed `.env.example` serves as the blueprint for local `.env` creation.
- Backend services use H2 in-memory databases for local development.
- API Gateway routes to internal services using Docker service names.
- Nginx root proxy exposes the frontend on a single host port.
- CORS is configured for common localhost dev URLs.
- Actuator endpoints exposed: health, info, metrics.
- JPA DDL is `create-drop` for clean local runs. Do not use in production.

## Getting Started

1. Prerequisites:
   - Windows with Docker Desktop running.
   - VS Code recommended.

2. Create your local environment file from the blueprint:
   - Copy the example:
     ```
     Copy-Item .env.example .env
     ```
   - Open `.env` and set values. At minimum, set a secure `SECURITY_JWT_SECRET` (32+ chars).
   - Keep `.env` private. It should be ignored by Git.

3. Build and run:
   ```
   docker compose up --build
   ```

4. Access the app:
   - Frontend via Nginx root proxy: http://localhost:80
   - API Gateway (direct dev port): http://localhost:8011
   - H2 console (per service, if enabled): http://localhost:<service-port>/h2-console

5. Tear down:
   ```
   docker compose down
   ```

## Using .env.example as a Blueprint

- `.env.example` is committed and pushed to the repo so collaborators can see required variables.
- Duplicate it to `.env` and customize for local development:
  - Security:
    - `SECURITY_JWT_SECRET`: long random string (32+ chars).
    - `SECURITY_JWT_EXPIRATION`: default `PT1H`.
  - Networking:
    - `NGINX_ROOT_PROXY_EXTERNAL_PORT`: default `80`.
    - `DOCKER_NETWORK_NAME`: default `lego-network`.
  - Frontend:
    - `VITE_API_GATEWAY_URL`: default `http://localhost:8011`.
    - `FRONTEND_SERVE_PORT`: default `5173`.
  - Gateway CORS:
    - `API_GATEWAY_CORS_ALLOWED_ORIGINS`: restrict as needed.
  - Microservice internal ports:
    - `API_GATEWAY_PORT`, `USER_SERVICE_PORT`, `MASTERDATA_SERVICE_PORT`, `INVENTORY_SERVICE_PORT`, `ORDER_PROCESSING_SERVICE_PORT`, `SIMAL_INTEGRATION_SERVICE_PORT`.
  - Databases (H2 in-memory):
    - `H2_DB_*` JDBC URLs with `DB_CLOSE_DELAY=-1`.
  - Spring Boot common:
    - `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop` (dev only).
    - `SPRING_JPA_SHOW_SQL=true`.
    - `SPRING_H2_CONSOLE_ENABLED=true`.
    - `SPRING_H2_CONSOLE_PATH=/h2-console`.
    - `SPRING_JMX_ENABLED=false`.
  - Logging:
    - `LOGGING_LEVEL_IO_LIFE=DEBUG`, `LOG_LEVEL_ROOT=INFO`.
  - Management:
    - `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health,info,metrics"`.
    - `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always`.
  - Internal service URLs for Gateway routing:
    - `API_GATEWAY_*_SERVICE_URL` using Docker service names and ports.
  - Docker Compose defaults:
    - `DOCKER_RESTART_POLICY=unless-stopped`, `DOCKER_MEMORY_LIMIT=512m`, `DOCKER_CPU_LIMIT=1.0`, `DOCKER_TAG=latest`.

## Development

- Frontend dev server:
  ```
  npm run dev --prefix frontend
  ```
  Ensure `VITE_API_GATEWAY_URL` points to the gateway.

- Logs:
  ```
  docker compose logs -f
  ```

