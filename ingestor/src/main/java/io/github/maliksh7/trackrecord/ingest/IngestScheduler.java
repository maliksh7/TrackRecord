package io.github.maliksh7.trackrecord.ingest;

import java.time.LocalDateTime;

import io.github.maliksh7.trackrecord.config.TrackRecordProperties;
import io.github.maliksh7.trackrecord.config.TrackRecordProperties.Station;
import io.github.maliksh7.trackrecord.parse.IrisTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polling schedule. Request budget (see docs/01-discovery-memo.md): with N stations,
 * plans cost ~N requests/hour once warmed up and /fchg costs N / pollIntervalMinutes per minute.
 * Example: 30 stations, 2-minute polling -> ~15.5 req/min against a 60 req/min plan.
 */
@Component
class IngestScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);

    private final TrackRecordProperties props;
    private final IngestService ingest;

    IngestScheduler(TrackRecordProperties props, IngestService ingest) {
        this.props = props;
        this.ingest = ingest;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onStartup() {
        if (!active()) {
            return;
        }
        log.info("Ingest active for {} stations, change poll every {}", props.stations().size(),
                props.ingest().changePollInterval());
        pollPlans();
    }

    /** Hourly, fetch the current hour plus the lookahead window; hours already held are skipped. */
    @Scheduled(cron = "0 5 * * * *", zone = "Europe/Berlin")
    void pollPlans() {
        if (!active()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(IrisTime.BERLIN);
        for (int offset = 0; offset <= props.ingest().planLookaheadHours(); offset++) {
            LocalDateTime slot = now.plusHours(offset);
            for (Station station : props.stations()) {
                ingest.ingestPlan(station, slot.toLocalDate(), slot.getHour());
            }
        }
    }

    @Scheduled(fixedDelayString = "${trackrecord.ingest.change-poll-interval:PT2M}", initialDelayString = "PT30S")
    void pollChanges() {
        if (!active()) {
            return;
        }
        props.stations().forEach(ingest::ingestChanges);
    }

    private boolean active() {
        if (!props.ingest().enabled()) {
            return false;
        }
        if (!props.dbApi().hasCredentials()) {
            log.warn("DB_CLIENT_ID / DB_API_KEY not set; skipping ingest. See README > Getting started.");
            return false;
        }
        return props.stations() != null && !props.stations().isEmpty();
    }
}
