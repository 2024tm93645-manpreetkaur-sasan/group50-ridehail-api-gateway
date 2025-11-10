# group50-ridehail-api-gateway

A lightweight API Gateway acting as the single external entry point for the RideHail microservices.  
It provides routing, correlation IDs, JSON logging, and basic rate limiting.

---

## What the Gateway Does

### 1. Routes External Requests
The gateway exposes `/api/...` routes and forwards them to backend services:

| External Path        | Forwards To           |
|----------------------|------------------------|
| `/api/trips/**`      | Trip Service           |
| `/api/riders/**`     | Rider Service          |
| `/api/payments/**`   | Payment Service        |

> Driver Service is internal-only and intentionally not exposed through the gateway.

---

### 2. Correlation ID
Each request gets an `X-Correlation-Id` that is:
- generated if missing
- propagated downstream
- logged in JSON format

This allows consistent tracing across services.

---

### 3. JSON Logging
All logs emitted from the gateway follow a clean JSON format, making them easy to parse and search.

---

### 4. Rate Limiting
A simple rate limiter protects API endpoints:
- `1 request/sec`
- `burst capacity: 2`
- keyed by `X-Api-Key`

---

---
## Deployment Model (Minimal)

### Trip Service
- Runs **inside Minikube**
- Exposed via `LoadBalancer` mapped to `127.0.0.1:9082`
- Gateway forwards `/api/trips/**` → `/v1/trips/**` inside the service

### Rider Service
- Runs in **local Docker Compose**
- Accessible at `http://localhost:9081`

### Payment Service
- Runs in **local Docker Compose**
- Accessible at `http://localhost:9083`

### Driver Service
- Internal service, also in Docker
- Not routed through the gateway
- Called directly by the Trip Service

### API Gateway
- Runs in **local Docker**
- Exposes external client endpoints at port `8888`

---

## Example Gateway Route

```properties
spring.cloud.gateway.routes[0].id=trip_service
spring.cloud.gateway.routes[0].uri=http://127.0.0.1:9082
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/trips/**
spring.cloud.gateway.routes[0].filters[0].name=RewritePath
spring.cloud.gateway.routes[0].filters[0].args.regexp=/api/trips(?<segment>/?.*)
spring.cloud.gateway.routes[0].filters[0].args.replacement=/v1/trips${segment}

