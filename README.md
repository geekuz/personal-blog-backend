# Personal Blog Backend

Spring Boot API implementing [`BACKEND_HANDOFF.md`](BACKEND_HANDOFF.md). It exposes published Markdown posts and their tags through public read endpoints. Protected admin endpoints create, inspect, update, and delete both draft and published posts.

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
preserved frontend Markdown through a forward-only migration. `V4` removes those
demo posts so production content can be managed exclusively through the admin API.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/personal_blog` | JDBC database URL |
| `DATABASE_USERNAME` | `blog` | Database user |
| `DATABASE_PASSWORD` | `blog` | Database password |
| `BLOG_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated exact CORS origins |
| `BLOG_ADMIN_API_KEY` | _(empty; admin API disabled)_ | Secret required in `X-Admin-API-Key` for every admin request |
| `BLOG_ADMIN_EMAIL` | _(empty; dashboard disabled)_ | Existing account email that receives the `ADMIN` role at startup |
| `SESSION_COOKIE_SECURE` | `false` | Set `true` in HTTPS production |
| `SESSION_COOKIE_SAME_SITE` | `lax` | Set `none` while frontend and API use different sites |
| `BLOG_FRONTEND_URL` | `http://localhost:5173` | Public frontend origin used in verification links |
| `RESEND_API_KEY` | _(empty)_ | Secret Resend API key used for transactional email |
| `BLOG_EMAIL_FROM` | `onboarding@resend.dev` | Verified Resend sender address |

Production should supply every database setting from its secret/configuration system. Hibernate validates the Flyway-managed schema and never updates it. Timestamps are stored and serialized in UTC.

The frontend should use:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Manage posts through the admin API

Generate a strong key and store it as `BLOG_ADMIN_API_KEY` in Render. Never put
the key in Git or in a frontend environment variable:

```bash
openssl rand -hex 32
```

Create a published post (omit `publishedAt` to publish it immediately):

```bash
curl -X POST 'https://personal-blog-backend-idiq.onrender.com/api/v1/admin/posts' \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-API-Key: YOUR_SECRET_KEY' \
  --data '{
    "slug": "my-first-real-post",
    "title": "My First Real Post",
    "summary": "A short description shown on the homepage.",
    "content": "# Hello\n\nWrite the full post here in **Markdown**.",
    "coverImageUrl": "https://images.example.com/my-first-post.jpg",
    "coverImageAlt": "Laptop displaying the finished blog post",
    "status": "PUBLISHED",
    "tags": [
      {"name": "Java", "slug": "java"},
      {"name": "Learning", "slug": "learning"}
    ]
  }'
```

Admin endpoints:

- `POST /api/v1/admin/posts` creates a post.
- `GET /api/v1/admin/posts/{slug}` returns a draft or published post.
- `PUT /api/v1/admin/posts/{slug}` replaces a post using the same JSON shape.
- `DELETE /api/v1/admin/posts/{slug}` permanently deletes a post.

Use `"status": "DRAFT"` to keep a post out of all public endpoints. Slugs must
be lowercase kebab-case. `publishedAt` accepts an optional ISO-8601 UTC timestamp,
for example `2026-08-17T07:00:00Z`. Cover images are optional; when used,
`coverImageUrl` and descriptive `coverImageAlt` must be provided together.

## User accounts

Authentication uses Spring Security, BCrypt password hashing, CSRF protection,
an HTTP-only session cookie, and JDBC-backed sessions that survive application
restarts. Public post endpoints remain anonymous.

- `GET /api/v1/auth/csrf` returns the CSRF header name and token.
- `POST /api/v1/auth/register` creates a `USER` account.
- `POST /api/v1/auth/login` starts a session.
- `GET /api/v1/auth/me` returns the current session state.
- `POST /api/v1/auth/logout` invalidates the session.
- `POST /api/v1/auth/verify-email` consumes a single-use verification token.
- `POST /api/v1/auth/verification/resend` sends a new link for the logged-in user.
- `POST /api/v1/auth/password/forgot` sends a reset link without revealing whether the account exists.
- `POST /api/v1/auth/password/reset` consumes a reset token and signs out every existing session.
- `POST /api/v1/auth/password/change` changes the logged-in user's password and signs out every session.

Verification tokens expire after 24 hours, are stored only as SHA-256 hashes,
and can be requested at most once per minute.

Password reset tokens expire after 30 minutes, are single-use, and are also
stored only as SHA-256 hashes. Registration, login, verification, recovery, and
password changes are rate-limited per client address; throttled responses use
HTTP 429 with a `Retry-After` header.

Configure Resend in production:

```text
BLOG_FRONTEND_URL=https://personal-blog-frontend-virid.vercel.app
RESEND_API_KEY=re_...
BLOG_EMAIL_FROM=onboarding@resend.dev
```

The Resend onboarding sender is suitable for initial testing with the Resend
account owner's email. Verify a domain in Resend and replace `BLOG_EMAIL_FROM`
before sending to general users.

For the current Vercel/Render deployment, set these Render variables:

```text
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=none
```

The frontend sends credentials explicitly. Moving the frontend and API onto
subdomains of one custom domain is recommended before relying on authentication
for a broad audience, because some browsers restrict third-party cookies.

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
- `V4` removes the demo posts and their unused tags.
- `V5` creates users, roles, and persistent Spring Session tables.
- `V6` creates single-use email verification tokens.

Never edit an applied production migration. Add a new versioned migration instead.

## Production image

```bash
./mvnw clean package
docker build -t personal-blog-backend .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL='jdbc:postgresql://db:5432/personal_blog' \
  -e DATABASE_USERNAME='...' -e DATABASE_PASSWORD='...' \
  -e BLOG_ALLOWED_ORIGINS='https://blog.example.com' \
  -e BLOG_ADMIN_API_KEY='...' \
  personal-blog-backend
```

Only Actuator health is exposed, without details. Admin writes are denied when
`BLOG_ADMIN_API_KEY` is empty and use constant-time key comparison when enabled.
