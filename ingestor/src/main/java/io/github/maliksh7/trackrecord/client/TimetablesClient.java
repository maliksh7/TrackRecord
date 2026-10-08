package io.github.maliksh7.trackrecord.client;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for the DB Timetables API (IRIS). It never throws on HTTP errors: every response,
 * good or bad, is returned so it can be landed in raw_response for later debugging.
 */
@Component
public class TimetablesClient {

    private static final DateTimeFormatter PLAN_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final RestClient restClient;
    private final RateLimiter rateLimiter;

    TimetablesClient(RestClient timetablesRestClient, RateLimiter dbApiRateLimiter) {
        this.restClient = timetablesRestClient;
        this.rateLimiter = dbApiRateLimiter;
    }

    /** Planned timetable for one station and one hour (local time). */
    public ApiResponse plan(String eva, LocalDate date, int hour) {
        return get("/plan/{eva}/{date}/{hour}", eva, PLAN_DATE.format(date), "%02d".formatted(hour));
    }

    /** All known changes for the station for the current day. */
    public ApiResponse fullChanges(String eva) {
        return get("/fchg/{eva}", eva);
    }

    /** Only changes from the last couple of minutes; cheaper to parse, but easy to miss updates. */
    public ApiResponse recentChanges(String eva) {
        return get("/rchg/{eva}", eva);
    }

    private ApiResponse get(String uri, Object... vars) {
        return RateLimiter.decorateSupplier(rateLimiter, () -> restClient.get()
                .uri(uri, vars)
                .exchange((request, response) -> new ApiResponse(
                        response.getStatusCode().value(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))))
                .get();
    }

    public record ApiResponse(int status, String body) {
        public boolean ok() {
            return status == 200;
        }
    }
}
