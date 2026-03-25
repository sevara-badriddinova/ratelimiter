# Distributed Rate Limiter

A production-grade distributed rate limiter built with Java/Spring Boot 
and Redis, implementing the token bucket algorithm with atomic operations.

## Architecture

- **Algorithm**: Token bucket with configurable capacity and refill rate
- **State**: Shared Redis instance ensures consistent enforcement across 
  multiple application instances
- **Atomicity**: Redis Lua scripts eliminate race conditions under 
  concurrent load
- **Observability**: Prometheus metrics + Grafana dashboard for real-time 
  monitoring

## Tech Stack

Java 17, Spring Boot 3.5, Redis, Docker, Prometheus, Grafana, k6

## How It Works

Each user gets a token bucket in Redis with two keys:
- `rate:{userId}:tokens` — current token count
- `rate:{userId}:lastRefill` — last refill timestamp

On every request, a Lua script atomically:
1. Reads current tokens and last refill time
2. Calculates tokens to add based on elapsed time
3. If tokens available → decrements and returns 200
4. If empty → returns 429

Since the script runs atomically in Redis, multiple app instances 
cannot create race conditions.

## Performance

Load tested with k6 (50 virtual users, 30 seconds):
- **Throughput**: 7,500 requests/second
- **P95 latency**: 11ms
- **Correctness**: 100% of responses were valid 200 or 429

## Running Locally

**Prerequisites**: Java 17, Docker, Maven
```bash
# Start Redis
docker run -d -p 6379:6379 redis

# Run the app
./mvnw spring-boot:run

# Start Prometheus
docker run -d -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# Start Grafana
docker run -d -p 3000:3000 grafana/grafana

# Load test
k6 run load-test.js
```

## API
```
GET /api/request
Headers: X-User-Id: {userId}

Response: 200 OK - request allowed
Response: 429 Too Many Requests - rate limit exceeded
```

## Monitoring

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Metrics endpoint: http://localhost:8080/actuator/prometheus

## What I Learned

- Distributed systems concurrency problems and how atomicity solves them
- Why Redis Lua scripts are necessary (not just convenient) for correctness
- How to observe a system under load and identify bottlenecks
- Redis becomes the bottleneck at high concurrency — sharding is the fix
