# Deployment and Operations

The Docker Compose configuration in the repository is intended for local development and verification, not direct production exposure. It publishes the database, backend, AI service, and frontend ports without TLS or production secret management.

## Before deploying

- Put a TLS-terminating reverse proxy or load balancer in front of the application. Do not expose PostgreSQL or internal service ports publicly.
- Provide unique, high-entropy values for `POSTGRES_PASSWORD`, `JWT_SECRET`, and `AI_SERVICE_API_KEY` through the deployment platform's secret store. Do not use the example defaults or commit a populated `.env` file.
- Set `FRONTEND_ORIGIN` to the exact HTTPS frontend origin and set `REFRESH_COOKIE_SECURE=true`.
- Configure the AI service provider and API credentials through secrets as needed. The default mock provider is for development and produces deterministic placeholder analysis.
- Persist and back up the PostgreSQL data and uploaded-document storage. Apply the documented retention and deletion policies to both.
- Restrict access to operational and API documentation endpoints according to the deployment's access policy.

## Health and verification

- The backend exposes `/actuator/health` for container health checks and `/actuator/health/liveness` plus `/actuator/health/readiness` for Spring Boot probes.
- The AI service exposes `/health`; the frontend container serves its static application on port 80.
- Run the repository CI checks before deployment:

```bash
npm --prefix requirement-analyzer-frontend ci
npm --prefix requirement-analyzer-frontend run typecheck
npm --prefix requirement-analyzer-frontend test
npm --prefix requirement-analyzer-frontend run build
mvn -f requirement-analyzer-backend/pom.xml verify
python3 -m pip install -r requirement-analyzer-ai-service/requirements.txt
python3 -m pytest requirement-analyzer-ai-service/tests
```

The frontend's analysis/review flow supports JSON, PDF, and Excel (`.xlsx`) exports for a selected analysis version. Export endpoints use the same project ownership checks as the JSON response.

## Operational limitations

The current Compose setup is a single-instance development topology. It does not configure production TLS, network isolation, managed backups, centralized logs/metrics, autoscaling, or a production-grade job broker. Establish those controls in the target platform before accepting production traffic.
