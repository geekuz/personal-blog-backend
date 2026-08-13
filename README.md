# Personal Blog Backend

Read-only Spring Boot API implementing [`BACKEND_HANDOFF.md`](BACKEND_HANDOFF.md). It exposes published Markdown posts and their tags; drafts are never returned by public queries.

## Requirements

- Java 21
- Docker with Compose (PostgreSQL and PostgreSQL integration tests)

## Run locally

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API is available at `http://localhost:8080/api/v1`. Swagger UI is at `http://localhost:8080/swagger-ui.html`; the OpenAPI document is at `/v3/api-docs`.

Useful requests:

```bash
curl 'http://localhost:8080/api/v1/posts?page=0&size=10'
curl 'http://localhost:8080/api/v1/posts?q=java&tag=learning'
curl 'http://localhost:8080/api/v1/posts/why-i-chose-react'
curl 'http://localhost:8080/api/v1/tags'
```

Flyway creates the schema and inserts the three original frontend posts. `V2`
provides the initial seed and `V3` replaces its representative content with the
preserved frontend Markdown through a forward-only migration.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/personal_blog` | JDBC database URL |
| `DATABASE_USERNAME` | `blog` | Database user |
| `DATABASE_PASSWORD` | `blog` | Database password |
| `BLOG_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated exact CORS origins |

Production should supply every database setting from its secret/configuration system. Hibernate validates the Flyway-managed schema and never updates it. Timestamps are stored and serialized in UTC.

The frontend should use:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Tests and build

```bash
./mvnw test
./mvnw clean package
```

API integration tests use a disposable PostgreSQL Testcontainer and automatically skip only when Docker is unavailable. Unit tests still run without Docker. The reading-time rule splits trimmed Markdown source on Unicode whitespace and returns `max(1, round(wordCount / 200))`.

## Database migrations

Migrations live in `src/main/resources/db/migration`:

- `V1` creates posts, tags, the many-to-many join table, constraints, and indexes.
- `V2` inserts the initial representative seed data.
- `V3` imports the three original frontend posts and removes the placeholders.

Never edit an applied production migration. Add a new versioned migration instead.

## Production image

```bash
./mvnw clean package
docker build -t personal-blog-backend .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL='jdbc:postgresql://db:5432/personal_blog' \
  -e DATABASE_USERNAME='...' -e DATABASE_PASSWORD='...' \
  -e BLOG_ALLOWED_ORIGINS='https://blog.example.com' \
  personal-blog-backend
```

Only Actuator health is exposed, without details. Admin authentication and write endpoints remain phase 2 as required by the handoff.
