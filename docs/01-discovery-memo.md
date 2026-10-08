# Discovery memo

> Written as if for a client. One page. Fill the _italic prompts_; keep what's already true.

**Client (fictional):** operations analyst at a regional transit authority, plus a corporate travel manager whose staff commute by rail.
**Author:** Muhammad Saad Hassan · **Date:** _YYYY-MM-DD_ · **Status:** draft

## 1. Problem
Punctuality is reported network-wide and monthly. That's too coarse to act on. The client can't see *which* stations, lines and hours lose the most time, or which connections their people can rely on.

## 2. Questions the client wants answered
1. Which stations / lines / hours lose the most delay minutes?
2. How reliable is a given connection (transfer with an X-minute buffer)?
3. Do delays propagate down a route, and by how much?
4. Can someone be warned *before* a connection they depend on breaks?

## 3. Success criteria (measurable)
| Criterion | Target | How measured |
|---|---|---|
| Ingest coverage | _e.g. ≥ 95% of planned stops have an actual time_ | `Coverage %` panel |
| Data freshness | _e.g. changes visible within 5 min_ | `ingest_health.last_fetch` |
| Metric validity | _Punctuality within ±X pp of DB's published figure, gap explained_ | docs/03-data-validation.md |
| Agent accuracy | _e.g. ≥ 85% on the eval set_ | docs/05-agent-evals.md |

## 4. Constraints
- **API budget:** DB Timetables free plan, **60 requests/minute**, shared by all jobs. We cap at 50.
- **Data licence:** CC BY 4.0, attribution required (DB, plus piebro dataset for backfill).
- **Official API only.** No scraping bahn.de.
- **Station-level data.** IRIS is a per-station board, not a per-train feed. Train journeys must be stitched together from stations.

## 5. Request budget
With **N** stations, a change-poll interval of **p** minutes and plans fetched hourly:

```
req/min ≈ N / p            (fchg)
        + N / 60           (one new plan hour per station per hour, after warm-up)
warm-up ≈ N × (lookahead + 1) requests at startup
```

| Stations (N) | Poll (p) | ≈ req/min | Headroom vs 50 |
|---|---|---|---|
| 8 (current config) | 2 min | 4.1 | plenty |
| 30 | 2 min | 15.5 | ok |
| 60 | 2 min | 31 | ok |
| 90 | 2 min | 46.5 | too tight, use p = 3 |

_Decision:_ _which stations, and why (hubs on the client's commuter corridors? spread across regions for comparison?)_

## 6. Open questions to resolve in week 1
- [ ] Does IRIS send `ct` for on-time trains, or only when there's a deviation? (This decides whether a missing actual means "on time" or "unknown".)
- [ ] Does a stop drop out of `/fchg` once the train has left? If so, how close to the real departure is our last observation?
- [ ] Are the configured EVA numbers correct? (Check each via `/station/{name}`.)
- [ ] How are replacement buses, extra trains (`cs=a`) and partial cancellations represented?

## 7. Out of scope (for now)
Passenger counts, ticketing, non-DB operators not in IRIS, predictions of future delays.
