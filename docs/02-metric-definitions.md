# Metric definitions

Source of truth for every number on the dashboard and every answer the agent gives.
Implemented in `ingestor/src/main/resources/db/migration/V3__metric_views.sql`. **Change both together.**

| Metric | Definition | View / column |
|---|---|---|
| **Delay (min)** | Actual minus planned, in minutes. Uses arrival if present, else departure (origin stations have no arrival). Can be negative (early). | `stop_delay.delay_min` |
| **Punctual** | Not cancelled **and** delay < 6 min. This matches DB's published rule (on time = less than 6 minutes late). | `punctuality_station_hour.punctual_stops` |
| **Punctuality %** | punctual ÷ (not cancelled **and** has an actual time). Stops with no actual time are *excluded*, not counted as on time. | `punctuality_pct` |
| **Coverage %** | stops with an actual ÷ (planned − cancelled). Low coverage means punctuality % is less trustworthy. | dashboard query |
| **Cancelled** | `cs = c` on the arrival or departure event. Excluded from punctuality (DB does the same; say so whenever you compare figures). | `stop_delay.cancelled` |
| **Delay minutes** | Sum of positive delays. Early trains don't offset late ones. | `delay_minutes_total` |
| **Platform change** | Changed platform differs from planned platform. | `stop_delay.platform_changed` |
| **Settling window** | Only stops planned more than 30 minutes ago count, so late updates can land first. | `punctuality_station_hour` filter |

## Planned (week 2)
| Metric | Draft definition |
|---|---|
| **Connection reliability** | For arrival A and departure D at the same station with planned buffer b: share of days where actual(D) − actual(A) ≥ minimum transfer time. _Decide the minimum transfer time per station, or use a flat 5 min, and document which._ |
| **Delay propagation** | Same train (category + number + date) across consecutive configured stations: Δdelay between stations. _Needs journey stitching across stations._ |

## Decisions log
| Date | Decision | Why |
|---|---|---|
| _YYYY-MM-DD_ | Stops with no actual time are excluded from punctuality | Unknown ≠ on time; coverage reported separately |
