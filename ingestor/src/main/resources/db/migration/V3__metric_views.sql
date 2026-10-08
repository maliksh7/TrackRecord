-- Metric layer. Definitions are documented in docs/02-metric-definitions.md; change both together.
-- Grafana and the (week 3) agent read ONLY these views, never the raw tables.

-- Latest known change per stop and event.
create view stop_event_latest as
select distinct on (stop_id, event_kind)
       stop_id, event_kind, changed_time, changed_platform, changed_status, observed_at
from change_event
order by stop_id, event_kind, observed_at desc;

-- One row per stop: plan vs latest actual.
create view stop_delay as
select s.stop_id,
       s.eva,
       st.name                                                              as station_name,
       s.category,
       s.train_number,
       s.line,
       s.filter_flags,
       s.planned_arrival,
       s.planned_departure,
       coalesce(s.planned_arrival, s.planned_departure)                     as planned_time,
       a.changed_time                                                       as actual_arrival,
       d.changed_time                                                       as actual_departure,
       extract(epoch from (a.changed_time - s.planned_arrival)) / 60.0      as arrival_delay_min,
       extract(epoch from (d.changed_time - s.planned_departure)) / 60.0    as departure_delay_min,
       -- Headline delay: arrival if we have it, else departure (origin stations have no arrival).
       coalesce(extract(epoch from (a.changed_time - s.planned_arrival)),
                extract(epoch from (d.changed_time - s.planned_departure))) / 60.0 as delay_min,
       coalesce(a.changed_status = 'c', false) or coalesce(d.changed_status = 'c', false) as cancelled,
       coalesce(a.changed_platform <> s.planned_arrival_platform, false)
           or coalesce(d.changed_platform <> s.planned_departure_platform, false)    as platform_changed
from stop s
join station st using (eva)
left join stop_event_latest a on a.stop_id = s.stop_id and a.event_kind = 'ar'
left join stop_event_latest d on d.stop_id = s.stop_id and d.event_kind = 'dp';

-- Punctuality per station and hour. DB rule: on time = less than 6 minutes late.
-- Only stops planned >30 min ago, so late-arriving updates have a chance to land.
-- hour_local is Berlin wall-clock (for "worst hour of day" analysis); hour_start is the same
-- instant as timestamptz (for Grafana's time filter).
create view punctuality_station_hour as
select eva,
       station_name,
       hour_local,
       hour_local at time zone 'Europe/Berlin'                                          as hour_start,
       count(*)                                                                         as planned_stops,
       count(*) filter (where cancelled)                                                as cancelled_stops,
       count(*) filter (where not cancelled and delay_min is not null)                  as stops_with_actuals,
       count(*) filter (where not cancelled and delay_min < 6)                          as punctual_stops,
       round(100.0 * count(*) filter (where not cancelled and delay_min < 6)
             / nullif(count(*) filter (where not cancelled and delay_min is not null), 0), 1) as punctuality_pct,
       round(avg(delay_min) filter (where not cancelled)::numeric, 2)                  as avg_delay_min,
       round(sum(greatest(delay_min, 0)) filter (where not cancelled)::numeric, 0)     as delay_minutes_total,
       count(*) filter (where platform_changed)                                         as platform_changes
from (select *, date_trunc('hour', planned_time at time zone 'Europe/Berlin') as hour_local
      from stop_delay
      where planned_time < now() - interval '30 minutes') d
group by eva, station_name, hour_local;

-- Data-quality panel: is the pipeline itself healthy?
create view ingest_health as
select endpoint,
       eva,
       max(fetched_at)                                                        as last_fetch,
       count(*) filter (where fetched_at > now() - interval '1 hour')          as calls_last_hour,
       count(*) filter (where fetched_at > now() - interval '1 hour' and http_status <> 200) as http_errors_last_hour,
       count(*) filter (where fetched_at > now() - interval '1 hour' and parse_status = 'failed') as parse_failures_last_hour
from raw_response
group by endpoint, eva;
