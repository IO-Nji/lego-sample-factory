# Development & Operations Guide

## Quick Start

### Prerequisites
- Docker Desktop (Windows)
- Git
- 8GB RAM recommended
- Port 80 available

### Installation Steps
1. Clone the repository
2. Copy `.env.example` to `.env` and set your JWT secret
3. Start with `start-factory.ps1` or `docker-compose up --build -d`
4. Access the app at http://localhost

### Default Test Accounts
| Username         | Password  | Role                | Access                        |
|------------------|-----------|---------------------|-------------------------------|
| lego_admin       | password  | ADMIN               | Full system access            |
| warehouse_user   | password  | PLANT_WAREHOUSE     | Customer order fulfillment    |
| modules_user     | password  | MODULES_SUPERMARKET | Module warehouse operations   |

### Shutdown
- `docker-compose down` (stop all)
- `docker-compose down -v` (reset databases)

---

## Local Development (Without Docker)
- Backend: `cd lego-factory-backend/<service>; .\mvnw spring-boot:run`
- Frontend: `cd lego-factory-frontend; npm install; npm run dev` (http://localhost:5173)
- Update API config in `lego-factory-frontend/src/api/apiConfig.js`

## Building & Rebuilding
- Backend: `mvnw clean package`
- Frontend: `npm run build`
- Docker: `docker-compose build --no-cache <service>`

## Logs & Health
- `docker-compose logs -f` (all)
- `docker-compose logs -f <service>` (specific)
- Health: `Invoke-RestMethod http://localhost:8011/actuator/health`

## Testing API Endpoints
- Use PowerShell/Invoke-RestMethod for login and authenticated requests

---

## Configuration

### Environment Variables
- See `.env.example` for all options (JWT, ports, DB, logging, CORS)

### Docker Network
- Internal: `lego-network` (services communicate via Docker DNS)
- External: `http://localhost:80`

### Database Access
- Each service: H2 in-memory DB, web console on service port (see table in original README)

---

## Troubleshooting

### Common Issues
- Port 80 in use: stop IIS or change port in `.env`
- Docker not starting: check Docker Desktop, logs, rebuild
- 404 on API: avoid double `/api/api` prefix
- JWT expired: re-login or implement refresh
- Data lost: H2 is in-memory by default
- CORS errors: update allowed origins in `.env`
- Service unhealthy: check health endpoints and logs

---

For roadmap, contributing, and standards, see [README.roadmap.md].