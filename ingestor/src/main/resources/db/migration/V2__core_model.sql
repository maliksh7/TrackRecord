-- Normalised model: station -> stop (planned) -> change_event (observed history) + stop_message.

create table station (
    eva        text primary key,
    name       text,
    first_seen timestamptz not null default now()
);

-- One row per train calling at a station (IRIS <s>), holding the PLANNED state from /plan.
create table stop (
    stop_id                    text primary key,  -- IRIS s@id, shared by /plan and /fchg
    eva                        text not null references station (eva),
    category                   text,              -- tl@c: ICE, IC, RE, RB, S, ...
    train_number               text,              -- tl@n
    operator                   text,              -- tl@o
    filter_flags               text,              -- tl@f: F long-distance, N regional, S S-Bahn
    trip_type                  text,              -- tl@t
    line                       text,              -- ar/dp@l
    planned_arrival            timestamptz,
    planned_departure          timestamptz,
    planned_arrival_platform   text,
    planned_departure_platform text,
    planned_arrival_path       text,              -- stations before this one, '|' separated
    planned_departure_path     text,              -- stations after this one, '|' separated
    first_seen                 timestamptz not null default now(),
    raw_response_id            bigint references raw_response (id)
);

create index stop_eva_departure_idx on stop (eva, planned_departure);
create index stop_eva_arrival_idx on stop (eva, planned_arrival);

-- Append-only history of what /fchg told us, one row per distinct change per event.
-- No FK to stop: changes can arrive before (or without) the matching plan.
create table change_event (
    id               bigserial primary key,
    stop_id          text        not null,
    eva              text        not null,
    event_kind       char(2)     not null check (event_kind in ('ar', 'dp')),
    observed_at      timestamptz not null,
    changed_time     timestamptz,           -- ct
    changed_platform text,                  -- cp
    changed_path     text,                  -- cpth
    changed_status   char(1),               -- cs: p planned, a added, c cancelled
    raw_response_id  bigint references raw_response (id)
);

create index change_event_latest_idx on change_event (stop_id, event_kind, observed_at desc);

-- Delay causes, quality notes, disruptions (IRIS <m>). Attached to the stop or to ar/dp.
create table stop_message (
    stop_id    text not null,
    message_id text not null,
    scope      text not null check (scope in ('s', 'ar', 'dp')),
    type       text,         -- m@t, e.g. d = delay cause, q = quality change, h = HIM disruption
    code       int,          -- m@c
    category   text,         -- m@cat (German text)
    priority   smallint,     -- m@pr
    message_ts timestamptz,  -- m@ts
    first_seen timestamptz not null default now(),
    primary key (stop_id, message_id, scope)
);
