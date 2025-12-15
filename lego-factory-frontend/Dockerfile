# lego-factory-app/frontend/Dockerfile
# Multi-stage build
FROM node:21-alpine AS builder

WORKDIR /app

# --- MODIFICATION START ---
# No need for ARG/ENV if we hardcode it for the Docker build
# The React app *inside* the container will always hit /api
# The nginx-root-proxy will handle routing /api to the actual API Gateway
# If your React app itself needs different API_GATEWAY_URLs for different build environments,
# you would manage that via Vite's own environment variable handling
# and potentially a custom entrypoint script.
# For this Docker setup, relative /api is the target.

# If you need to explicitly pass this to Vite's build, you would add it to the RUN npm run build command.
# However, for production/docker deployment, '/api' is often hardcoded in the build
# and the outer proxy handles the rewrite.

# If your package.json build script doesn't pick up VITE_API_GATEWAY_URL from ARG,
# you need to explicitly pass it during the build command.
# --- MODIFICATION END ---

# Copy package files
COPY package*.json ./

# Install dependencies (including dev dependencies for build)
RUN npm ci

# Copy source
COPY . .

# Build application
# --- MODIFICATION START ---
# Explicitly set VITE_API_GATEWAY_URL to '/api' for the build step.
# This ensures the compiled React app always makes relative API calls.
RUN VITE_API_GATEWAY_URL=/api npm run build
# --- MODIFICATION END ---

# Production stage
FROM nginx:alpine

# Install wget for health checks
RUN apk add --no-cache wget

# Copy nginx config
# --- MODIFICATION START ---
# Use the internal frontend nginx.conf, not a generic default.conf.
# Assuming you have a frontend/nginx.conf like discussed before.
COPY nginx.conf /etc/nginx/conf.d/frontend.conf
# --- MODIFICATION END ---

# Copy built app
COPY --from=builder /app/dist /usr/share/nginx/html

# Create health endpoint (this health endpoint is for the Nginx server itself)
RUN echo 'server { listen 80; location /health { return 200 "healthy\n"; add_header Content-Type text/plain; } }' > /etc/nginx/conf.d/health.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
