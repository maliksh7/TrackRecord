# TrackRecord

Portfolio project run like a client engagement: DB Timetables API → Spring Boot ingestor → Postgres metric views → Grafana, with an LLM agent planned for week 3. See README.md for the architecture and docs/ for the engagement documents.

## Commands
- Build + tests: `cd ingestor && mvn verify`
- Local stack: `docker compose up -d` (Postgres + Grafana); ingestor via `mvn spring-boot:run` with `.env` sourced

## Conventions
- Java 25, Spring Boot 4.1, `JdbcClient` (no JPA), Flyway migrations. Never edit an applied migration; add a new one.
- Metric definitions live in `V3__metric_views.sql` **and** `docs/02-metric-definitions.md`. Keep them in sync.
- Raw API responses are always landed in `raw_response` before parsing.
- All DB API calls go through `TimetablesClient` (shared rate limiter, 60 req/min free plan).
- IRIS times are `yyMMddHHmm` Berlin local time: always parse via `IrisTime`.
- Never commit `.env` or API keys. Test fixtures under `src/test/resources/fixtures` are synthetic unless noted.
- Don't put findings or numbers in README/docs that haven't actually been measured.
