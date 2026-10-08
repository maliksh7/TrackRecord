package io.github.maliksh7.trackrecord.client;

import java.time.Duration;

import io.github.maliksh7.trackrecord.config.TrackRecordProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
class ClientConfig {

    @Bean
    RestClient timetablesRestClient(RestClient.Builder builder, TrackRecordProperties props) {
        var api = props.dbApi();
        return builder
                .baseUrl(api.baseUrl())
                .defaultHeader("DB-Client-Id", nullToEmpty(api.clientId()))
                .defaultHeader("DB-Api-Key", nullToEmpty(api.apiKey()))
                .defaultHeader("Accept", MediaType.APPLICATION_XML_VALUE)
                .build();
    }

    /** One shared budget across all jobs, so plan + change polling together never exceed the plan limit. */
    @Bean
    RateLimiter dbApiRateLimiter(TrackRecordProperties props) {
        var config = RateLimiterConfig.custom()
                .limitForPeriod(props.dbApi().requestsPerMinute())
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMinutes(2))
                .build();
        return RateLimiter.of("db-timetables", config);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
