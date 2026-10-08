package io.github.maliksh7.trackrecord.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "trackrecord")
public record TrackRecordProperties(DbApi dbApi, Ingest ingest, List<Station> stations) {

    /**
     * DB API Marketplace credentials and limits. The free Timetables plan allows 60 requests/minute;
     * we default below that to leave headroom for manual debugging calls.
     */
    public record DbApi(
            @DefaultValue("https://apis.deutschebahn.com/db-api-marketplace/apis/timetables/v1") String baseUrl,
            String clientId,
            String apiKey,
            @DefaultValue("50") int requestsPerMinute) {

        public boolean hasCredentials() {
            return clientId != null && !clientId.isBlank() && apiKey != null && !apiKey.isBlank();
        }
    }

    public record Ingest(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("2") int planLookaheadHours,
            @DefaultValue("PT2M") Duration changePollInterval) {
    }

    /** A station we poll, identified by its EVA number. */
    public record Station(String eva, String name) {
    }
}
