-- Landing zone: every API response, exactly as received, before any parsing.
create table raw_response (
    id           bigserial primary key,
    endpoint     text        not null check (endpoint in ('plan', 'fchg', 'rchg')),
    eva          text        not null,
    slot_date    date,        -- /plan only: the requested date
    slot_hour    smallint,    -- /plan only: the requested hour (local time)
    fetched_at   timestamptz not null default now(),
    http_status  int         not null,
    body         text,
    parse_status text        not null default 'pending'
                 check (parse_status in ('pending', 'ok', 'failed', 'skipped')),
    parse_error  text
);

create index raw_response_lookup_idx on raw_response (endpoint, eva, fetched_at desc);
create index raw_response_plan_slot_idx on raw_response (eva, slot_date, slot_hour) where endpoint = 'plan';
