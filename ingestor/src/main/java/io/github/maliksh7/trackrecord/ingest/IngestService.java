package io.github.maliksh7.trackrecord.ingest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import io.github.maliksh7.trackrecord.client.TimetablesClient;
import io.github.maliksh7.trackrecord.client.TimetablesClient.ApiResponse;
import io.github.maliksh7.trackrecord.config.TrackRecordProperties.Station;
import io.github.maliksh7.trackrecord.parse.IrisTime;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Stop;
import io.github.maliksh7.trackrecord.parse.TimetableParser;
import io.github.maliksh7.trackrecord.store.RawResponseRepository;
import io.github.maliksh7.trackrecord.store.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fetch -> land raw -> parse -> normalise. The raw insert is committed on its own, so a parser bug
 * never costs us data: failed rows stay in raw_response with parse_status = 'failed' for re-processing.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final TimetablesClient client;
    private final TimetableParser parser;
    private final RawResponseRepository raw;
    private final TimetableRepository timetables;
    private final TransactionTemplate tx;

    IngestService(TimetablesClient client, TimetableParser parser, RawResponseRepository raw,
                  TimetableRepository timetables, TransactionTemplate tx) {
        this.client = client;
        this.parser = parser;
        this.raw = raw;
        this.timetables = timetables;
        this.tx = tx;
    }

    public void ingestPlan(Station station, LocalDate date, int hour) {
        if (raw.hasPlan(station.eva(), date, hour)) {
            return;
        }
        ApiResponse response = client.plan(station.eva(), date, hour);
        long rawId = raw.insert("plan", station.eva(), date, hour, response.status(), response.body());
        if (!response.ok()) {
            log.warn("plan {} {} {}:00 -> HTTP {}", station.name(), date, hour, response.status());
            raw.markParsed(rawId, "skipped", "HTTP " + response.status());
            return;
        }
        process(rawId, response.body(), station, timetable -> {
            for (Stop stop : timetable.stops()) {
                timetables.upsertPlannedStop(stop, rawId);
            }
            log.info("plan {} {} {}:00 -> {} stops", station.name(), date, hour, timetable.stops().size());
        });
    }

    public void ingestChanges(Station station) {
        ApiResponse response = client.fullChanges(station.eva());
        long rawId = raw.insert("fchg", station.eva(), null, null, response.status(), response.body());
        if (!response.ok()) {
            log.warn("fchg {} -> HTTP {}", station.name(), response.status());
            raw.markParsed(rawId, "skipped", "HTTP " + response.status());
            return;
        }
        OffsetDateTime observedAt = OffsetDateTime.now(IrisTime.BERLIN);
        process(rawId, response.body(), station, timetable -> {
            int written = 0;
            for (Stop stop : timetable.stops()) {
                if (timetables.appendChange(stop, "ar", stop.arrival(), observedAt, rawId)) written++;
                if (timetables.appendChange(stop, "dp", stop.departure(), observedAt, rawId)) written++;
                stop.messages().forEach(m -> timetables.upsertMessage(stop.id(), m));
            }
            log.info("fchg {} -> {} stops, {} new change rows", station.name(), timetable.stops().size(), written);
        });
    }

    private void process(long rawId, String body, Station station, Consumer<ParsedTimetable> handler) {
        try {
            ParsedTimetable timetable = parser.parse(body, station.eva());
            tx.executeWithoutResult(status -> {
                String name = timetable.stationName() != null ? timetable.stationName() : station.name();
                timetables.upsertStation(station.eva(), name);
                if (timetable.eva() != null && !timetable.eva().equals(station.eva())) {
                    log.warn("Configured EVA {} but API answered for {}", station.eva(), timetable.eva());
                    timetables.upsertStation(timetable.eva(), name);
                }
                handler.accept(timetable);
            });
            raw.markParsed(rawId, "ok", null);
        } catch (RuntimeException e) {
            log.error("Failed to process raw_response {} for {}: {}", rawId, station.name(), e.getMessage());
            raw.markParsed(rawId, "failed", e.getMessage());
        }
    }
}
