# TrackRecord

**How reliable is German rail, really?** Delay analytics and connection-risk alerts built on Deutsche Bahn open data.

> 🚧 Work in progress. This README is written as a case study and gets filled in as the project
> progresses. Sections marked _TBD_ are placeholders. No results are claimed until they're measured.

---

## The problem
DB publishes punctuality as one network-wide number per month. An operations analyst or a commuter
can't act on that. They need to know **which stations, lines and hours** lose time, and **which
connections can be trusted**. → [Discovery memo](docs/01-discovery-memo.md)

## Approach
```
DB Timetables API ──► Spring Boot ingestor ──► Postgres
 (plan / fchg)          - rate-limited          raw_response (landing, as received)
                          (60 req/min plan)     station · stop · change_event · stop_message
piebro dataset ──────► backfill (planned) ──►   metric views (punctuality, delay, health)
                                                    │
                              ┌─────────────────────┼─────────────────────┐
                              ▼                     ▼                     ▼
                       Grafana dashboard    "Ask the data" agent   Commute alert agent
                                              (planned)              (planned)
```

Design choices worth noting:
- **Land raw first.** Every API response is stored before parsing, so a parser bug never loses data. Fix it and re-parse.
- **Changes are history, not overwrites.** `change_event` is append-only and de-duplicated, so we can see how a delay evolved.
- **One shared request budget** across all jobs, sized in the [discovery memo](docs/01-discovery-memo.md#5-request-budget).
- **Metrics live in SQL views**, with definitions in [docs/02-metric-definitions.md](docs/02-metric-definitions.md). The dashboard and the agent both read the same views, through a read-only role.

## Findings
_TBD: e.g. worst station/hour, least reliable connections, how our figures compare with DB's._

## Data quality & limitations
_TBD_ → [Data validation note](docs/03-data-validation.md)

---

## Getting started

**Prerequisites:** Java 25, Maven 3.9+, Docker.

1. **Get API credentials.** Register at [developers.deutschebahn.com](https://developers.deutschebahn.com), create an application and subscribe it to **Timetables** (free plan).
2. **Configure:**
   ```bash
   cp .env.example .env   # then fill in DB_CLIENT_ID, DB_API_KEY and the passwords
   ```
3. **Start Postgres + Grafana:**
   ```bash
   docker compose up -d
   ```
4. **Run the ingestor** (it creates the schema via Flyway on first start):
   ```bash
   cd ingestor && set -a && source ../.env && set +a && mvn spring-boot:run
   ```
5. Open Grafana at http://localhost:3000 (admin / `GRAFANA_ADMIN_PASSWORD`) → *TrackRecord – Overview*.

Without credentials the ingestor still starts. It logs a warning and skips polling.

**Tests:** `cd ingestor && mvn verify`

## Repository layout
```
ingestor/          Spring Boot service: API client, XML parser, scheduler, Flyway migrations
infra/postgres/    DB init (read-only role for Grafana / agent)
infra/grafana/     Provisioned datasource + dashboard
docs/              Discovery memo, metric definitions, data validation, runbook, agent evals
```

## Roadmap
- [x] Week 1: Ingestor (plan + fchg), raw landing, normalised model, rate limiting, CI
- [ ] Week 1: Verify EVA numbers and open questions with real responses; backfill from piebro dataset
- [ ] Week 2: Connection-reliability and delay-propagation metrics; validation against DB's published figures
- [ ] Week 3: "Ask the data" agent with evals; commute alert agent; AWS deployment; demo video

## Team
| Who | Focus |
|---|---|
| [Muhammad Saad Hassan](https://github.com/maliksh7) | _TBD_ |
| [Arslan Zafar](https://github.com/arslanzafar-pro) | _TBD_ |

How we work: [CONTRIBUTING.md](CONTRIBUTING.md)

## Data & attribution
- Timetable data: **Deutsche Bahn AG**, via the DB API Marketplace (Timetables API), licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- Historical backfill (planned): [piebro/deutsche-bahn-data](https://huggingface.co/datasets/piebro/deutsche-bahn-data), CC BY 4.0.

This is an independent project, not affiliated with or endorsed by Deutsche Bahn AG.
