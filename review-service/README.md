# review-service (sample)

This is a minimal Review service scaffold for v-tourhub (SOA-style).

Features:
- Stores reviews (id, tourId, userId, rating 1-5, comment, timestamps)
- CRUD endpoints + list by tour + average rating endpoint
- PostgreSQL (docker profile) and `application.yml` ready
- Dockerfile matching project multi-stage build conventions

Endpoints (via gateway: /api/reviews):
- POST /api/reviews -> create
- GET /api/reviews/{id} -> get
- PUT /api/reviews/{id} -> update
- DELETE /api/reviews/{id} -> delete
- GET /api/reviews/tour/{tourId} -> list reviews for a tour
- GET /api/reviews/tour/{tourId}/average -> average rating (double)

Run locally (basic):
- Build: `mvn -f review-service/pom.xml package`
- Run: `java -jar review-service/target/*.jar`

Quick curl examples (when service reachable on port 9003 or via gateway at /api/reviews):

Create a review:
```
curl -X POST http://localhost:9003/api/reviews \
 -H "Content-Type: application/json" \
 -d '{"tourId":123,"userId":10,"rating":5,"comment":"Great tour!"}'
```

List reviews for a tour:
```
curl http://localhost:9003/api/reviews/tour/123
```

Get average rating:
```
curl http://localhost:9003/api/reviews/tour/123/average
```

Run with docker-compose (after adding service to root `docker-compose.yml`):
- `docker compose up -d review-service postgres-review`

Notes:
- Endpoints are public by default; adjust security if you want JWT protection.
- For production, move DB credentials to secrets and set secure JPA ddl settings.
