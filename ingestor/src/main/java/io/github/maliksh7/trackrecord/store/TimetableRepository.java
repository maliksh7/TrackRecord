package io.github.maliksh7.trackrecord.store;

import java.time.OffsetDateTime;

import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Event;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Message;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Stop;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.TripLabel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TimetableRepository {

    private final JdbcClient jdbc;

    TimetableRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertStation(String eva, String name) {
        jdbc.sql("""
                        insert into station (eva, name) values (:eva, :name)
                        on conflict (eva) do update set name = coalesce(excluded.name, station.name)""")
                .param("eva", eva)
                .param("name", name)
                .update();
    }

    /** Planned data from /plan. Later plan fetches for the same stop overwrite the planned fields. */
    public void upsertPlannedStop(Stop stop, long rawResponseId) {
        TripLabel tl = stop.tripLabel();
        Event ar = stop.arrival();
        Event dp = stop.departure();
        jdbc.sql("""
                        insert into stop (stop_id, eva, category, train_number, operator, filter_flags, trip_type, line,
                                          planned_arrival, planned_departure,
                                          planned_arrival_platform, planned_departure_platform,
                                          planned_arrival_path, planned_departure_path, raw_response_id)
                        values (:id, :eva, :category, :number, :operator, :flags, :tripType, :line,
                                :pa, :pd, :ppa, :ppd, :ptha, :pthd, :raw)
                        on conflict (stop_id) do update set
                            category = excluded.category, train_number = excluded.train_number,
                            operator = excluded.operator, filter_flags = excluded.filter_flags,
                            trip_type = excluded.trip_type, line = excluded.line,
                            planned_arrival = excluded.planned_arrival, planned_departure = excluded.planned_departure,
                            planned_arrival_platform = excluded.planned_arrival_platform,
                            planned_departure_platform = excluded.planned_departure_platform,
                            planned_arrival_path = excluded.planned_arrival_path,
                            planned_departure_path = excluded.planned_departure_path,
                            raw_response_id = excluded.raw_response_id""")
                .param("id", stop.id())
                .param("eva", stop.eva())
                .param("category", tl == null ? null : tl.category())
                .param("number", tl == null ? null : tl.number())
                .param("operator", tl == null ? null : tl.operator())
                .param("flags", tl == null ? null : tl.filterFlags())
                .param("tripType", tl == null ? null : tl.tripType())
                .param("line", ar != null && ar.line() != null ? ar.line() : dp == null ? null : dp.line())
                .param("pa", ar == null ? null : ar.plannedTime())
                .param("pd", dp == null ? null : dp.plannedTime())
                .param("ppa", ar == null ? null : ar.plannedPlatform())
                .param("ppd", dp == null ? null : dp.plannedPlatform())
                .param("ptha", ar == null ? null : ar.plannedPath())
                .param("pthd", dp == null ? null : dp.plannedPath())
                .param("raw", rawResponseId)
                .update();
    }

    /**
     * Appends a change observation, but only if it differs from the latest one we hold for this
     * stop/event. /fchg returns the full day's changes on every poll, so without this the table would
     * grow by the same rows every two minutes. Returns true if a row was written.
     */
    public boolean appendChange(Stop stop, String eventKind, Event e, OffsetDateTime observedAt, long rawResponseId) {
        if (e == null || !e.hasChange()) {
            return false;
        }
        int rows = jdbc.sql("""
                        insert into change_event (stop_id, eva, event_kind, observed_at, changed_time,
                                                  changed_platform, changed_path, changed_status, raw_response_id)
                        select :id, :eva, :kind, :observedAt, :ct, :cp, :cpth, :cs, :raw
                        where not exists (
                            select 1 from (
                                select changed_time, changed_platform, changed_path, changed_status
                                from change_event
                                where stop_id = :id and event_kind = :kind
                                order by observed_at desc limit 1) latest
                            where latest.changed_time is not distinct from :ct
                              and latest.changed_platform is not distinct from :cp
                              and latest.changed_path is not distinct from :cpth
                              and latest.changed_status is not distinct from :cs)""")
                .param("id", stop.id())
                .param("eva", stop.eva())
                .param("kind", eventKind)
                .param("observedAt", observedAt)
                .param("ct", e.changedTime())
                .param("cp", e.changedPlatform())
                .param("cpth", e.changedPath())
                .param("cs", e.changedStatus())
                .param("raw", rawResponseId)
                .update();
        return rows > 0;
    }

    public void upsertMessage(String stopId, Message m) {
        if (m.id() == null) {
            return;
        }
        jdbc.sql("""
                        insert into stop_message (stop_id, message_id, scope, type, code, category, priority, message_ts)
                        values (:stopId, :id, :scope, :type, :code, :category, :priority, :ts)
                        on conflict (stop_id, message_id, scope) do nothing""")
                .param("stopId", stopId)
                .param("id", m.id())
                .param("scope", m.scope())
                .param("type", m.type())
                .param("code", m.code())
                .param("category", m.category())
                .param("priority", m.priority())
                .param("ts", m.timestamp())
                .update();
    }
}
