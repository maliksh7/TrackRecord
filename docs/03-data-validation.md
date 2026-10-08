# Data validation note

> Goal: show where our numbers can be trusted, where they can't, and why they differ from DB's.
> FDE interviewers care about this page more than the dashboard.

## 1. Comparison with DB's published punctuality
| Month | Our punctuality % (sampled stations) | DB published % | Gap | Explanation |
|---|---|---|---|---|
| _…_ | | | | |

Expected reasons for a gap. Confirm or rule out each one:
- **Sampling:** we cover _N_ big hubs; DB covers every stop. Hubs are often worse than average.
- **Measuring point:** _Does DB measure at every stop or only at selected points? Check DB's methodology page and cite it._
- **Coverage:** stops without an actual time are excluded (see metric definitions).
- **Long-distance vs regional mix:** DB publishes them separately; split ours by `filter_flags` the same way.

## 2. Known data issues
| Issue | Impact | Handling | Status |
|---|---|---|---|
| Autumn DST hour is ambiguous in `yyMMddHHmm` | Delays wrong by ±60 min for 02:00–02:59 on one night a year | Resolve to earlier offset; flag outliers | covered by `IrisTimeTest` |
| Trains crossing midnight | Naive date logic gives −1439 min delays | Full timestamps, not times of day | covered by `IrisTimeTest` |
| Changes can arrive before the plan | Orphan `change_event` rows | No FK; reconcile in views | by design |
| Stops may drop out of `/fchg` after departure | Last observed `ct` may not be the final one | _measure: last observed_at vs actual time_ | open |
| `ct` missing for on-time trains? | Coverage looks low | _verify in week 1_ | open |
| _…_ | | | |

## 3. Checks to run
```sql
-- Implausible delays (likely parsing or DST problems)
select * from stop_delay where delay_min < -30 or delay_min > 600;

-- Orphan changes (change seen, plan never fetched)
select count(*) from change_event c left join stop s using (stop_id) where s.stop_id is null;

-- Parse failures to investigate
select id, endpoint, eva, parse_error from raw_response where parse_status = 'failed' order by id desc limit 20;
```
