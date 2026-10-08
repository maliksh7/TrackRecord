# Contributing

TrackRecord is built by two people. These rules keep us out of each other's way and keep the history readable for anyone reviewing it later.

## Setup (each of us, separately)
1. Register your **own** app at [developers.deutschebahn.com](https://developers.deutschebahn.com) and subscribe it to Timetables. Don't share keys: each key has its own 60 req/min budget.
2. `cp .env.example .env`, fill it in, then `docker compose up -d`. Everyone gets their own local Postgres + Grafana.
3. `cd ingestor && mvn verify` should pass before you start.

## Workflow
- **No direct pushes to `master`.** Branch, open a PR, and get the other person's review before merging.
- Branch names: `feat/…`, `fix/…`, `docs/…`, `data/…` (e.g. `feat/connection-reliability`).
- Link the PR to its issue (`Closes #12`). Pick up work by assigning yourself the issue first.
- Keep PRs small enough to review in ~15 minutes.
- Commit messages: imperative and specific ("Add stop_delay view", not "updates").

## Rules that matter
- **Metrics:** changing a view in a migration means updating `docs/02-metric-definitions.md` in the same PR.
- **Migrations:** never edit one that's been merged. Add a new `V<n>__…sql`.
- **Numbers:** don't put a finding in the README or docs unless it was measured. Note the date and the data window.
- **Secrets:** never commit `.env`, API keys or real raw captures containing anything sensitive. Fixtures in `src/test/resources/fixtures` are synthetic unless the file says otherwise.
- **AI tooling:** fine to use (we use Claude Code). `CLAUDE.md` is our shared context for it, so update it when conventions change.
