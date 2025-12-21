# analytics-service (sample)

This service computes simple tour metrics by querying `review-service`.

Endpoints (via gateway: /api/analytics):
- GET /api/analytics/tour/{tourId}/summary -> { tourId, totalReviews, averageRating }
- GET /api/analytics/tour/{tourId}/distribution -> { rating -> count }

Run locally (basic):
- Build: `mvn -f analytics-service/pom.xml package`
- Run: `java -jar analytics-service/target/*.jar`

Docker Compose (after adding to root `docker-compose.yml`):
- `docker compose up -d analytics-service`

Notes:
- Service is stateless and queries `review-service` at `/api/reviews/tour/{tourId}`. For production we'd use events or a dedicated analytics DB.
- Endpoints are public (no auth). Adjust for your security model if needed.
