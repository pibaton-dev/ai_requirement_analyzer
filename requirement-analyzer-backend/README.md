# Requirement Analyzer Backend

Java 21 and Spring Boot modular monolith. This service owns user accounts, projects, and future business records.

## Implemented modules

- Identity: BCrypt password hashing, USER role on registration, signed short-lived JWT access tokens, rotating refresh tokens stored as SHA-256 hashes in HttpOnly/SameSite cookies, login/logout/refresh, and profile read/update.
- Projects: owner-scoped create/list/read/update/archive/delete APIs. Delete is soft; archived projects are read-only. Lists are paginated and capped at 100 results.
- Database: PostgreSQL with Flyway migrations. Hibernate validates the migrated schema and does not create tables.
- API errors: Spring ProblemDetail responses for domain and validation failures. Swagger UI is available at `/swagger-ui/index.html`.

Registration does not accept or grant ADMIN. A controlled admin bootstrap workflow is not part of this milestone. Every project lookup is scoped by authenticated owner ID; inaccessible projects return the same not-found result as missing projects.

## Local development

Run PostgreSQL through the repository's Docker Compose stack, then start Spring Boot with Java 21:

```bash
mvn spring-boot:run
```

The service reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. `JWT_SECRET` must be a high-entropy secret in deployed environments; the checked-in fallback is only for local development. Set `REFRESH_COOKIE_SECURE=true` behind HTTPS.

API base path: `/api/v1`. Health: `/api/v1/health`. OpenAPI UI: `/swagger-ui/index.html`.

## Tests

```bash
mvn verify
```

Integration tests run Flyway migrations against an isolated H2 database in PostgreSQL compatibility mode. Production uses PostgreSQL; Compose startup also validates the real PostgreSQL schema.