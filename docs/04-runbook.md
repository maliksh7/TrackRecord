# Runbook

For whoever operates TrackRecord after handover.

## Components
| Component | Where | Health check |
|---|---|---|
| Ingestor (Spring Boot) | `ingestor/` · port 8080 | `GET /actuator/health` |
| Postgres 17 | Docker `postgres` · port 5432 | `pg_isready` |
| Grafana | Docker `grafana` · port 3000 | "Data quality" panel |

## Routine
- **Is data flowing?** Check the Grafana "Data quality: ingest health" panel. `last_fetch` should be within `change-poll-interval` + a few seconds for every station.
- **Add a station:** look up its EVA via `/station/{name}`, add it to `trackrecord.stations` in `application.yml`, re-check the request budget (docs/01-discovery-memo.md §5), then restart.

## Incidents
| Symptom | Likely cause | Action |
|---|---|---|
| `http_errors_last_hour` > 0 with 401 | Credentials wrong, expired or rotated | Check `DB_CLIENT_ID` / `DB_API_KEY` in the DB API Marketplace; restart |
| 429 responses | Over the 60 req/min budget (another client sharing the key?) | Lower `DB_REQUESTS_PER_MINUTE`, raise the poll interval |
| `parse_failures_last_hour` > 0 | API format changed, or an error page came back instead of XML | Inspect `raw_response.body` for the failed ids; fix the parser; re-parse (raw data is kept) |
| Dashboard flat / empty | Ingest disabled or no credentials | Ingestor logs: "skipping ingest" warning |
| Disk growing | `raw_response` keeps every body | Apply retention (below) |

## Retention
`raw_response` stores every API body. Decide on a retention policy, e.g. delete `fchg` bodies older than 30 days once `parse_status = 'ok'`, and document it here.

## Rollback
Flyway migrations are forward-only. To roll back a bad view change, add a new migration that restores the previous definition. Never edit an applied migration.
