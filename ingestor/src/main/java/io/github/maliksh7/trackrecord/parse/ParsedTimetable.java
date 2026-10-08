package io.github.maliksh7.trackrecord.parse;

import java.time.OffsetDateTime;
import java.util.List;

/** Parsed form of an IRIS {@code <timetable>} document (plan, fchg or rchg). */
public record ParsedTimetable(String eva, String stationName, List<Stop> stops) {

    /** {@code <s>}: one train calling at this station. {@code id} links plan and change responses. */
    public record Stop(String id, String eva, TripLabel tripLabel, Event arrival, Event departure, List<Message> messages) {
    }

    /** {@code <tl>}: f = filter flags (F long-distance, N regional, S S-Bahn), t = trip type, o = operator, c = category, n = number. */
    public record TripLabel(String filterFlags, String tripType, String operator, String category, String number) {
    }

    /**
     * {@code <ar>} / {@code <dp>}. Planned fields (pt, pp, ppth, l) come from /plan; changed fields
     * (ct, cp, cpth, cs) come from /fchg and /rchg. cs: p = planned, a = added, c = cancelled.
     */
    public record Event(
            OffsetDateTime plannedTime, String plannedPlatform, String plannedPath, String line,
            OffsetDateTime changedTime, String changedPlatform, String changedPath, String changedStatus) {

        public boolean hasChange() {
            return changedTime != null || changedPlatform != null || changedPath != null || changedStatus != null;
        }
    }

    /** {@code <m>}: delay causes, quality notes and disruptions. scope = where it was attached: s, ar or dp. */
    public record Message(String id, String scope, String type, Integer code, String category, Integer priority,
                          OffsetDateTime timestamp) {
    }
}
