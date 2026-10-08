package io.github.maliksh7.trackrecord.store;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Landing zone: every API response is stored as received, before parsing. If the parser or the data
 * model turns out to be wrong, we re-parse from here instead of having lost the data.
 */
@Repository
public class RawResponseRepository {

    private final JdbcClient jdbc;

    RawResponseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String endpoint, String eva, LocalDate slotDate, Integer slotHour, int httpStatus, String body) {
        return jdbc.sql("""
                        insert into raw_response (endpoint, eva, slot_date, slot_hour, http_status, body)
                        values (:endpoint, :eva, :slotDate, :slotHour, :status, :body)
                        returning id""")
                .param("endpoint", endpoint)
                .param("eva", eva)
                .param("slotDate", slotDate)
                .param("slotHour", slotHour)
                .param("status", httpStatus)
                .param("body", body)
                .query(Long.class)
                .single();
    }

    public void markParsed(long id, String status, String error) {
        jdbc.sql("update raw_response set parse_status = :status, parse_error = :error where id = :id")
                .param("status", status)
                .param("error", error)
                .param("id", id)
                .update();
    }

    /** True if we already hold a successful plan response for this station/hour, so we can skip the call. */
    public boolean hasPlan(String eva, LocalDate date, int hour) {
        return jdbc.sql("""
                        select exists (select 1 from raw_response
                                       where endpoint = 'plan' and eva = :eva and slot_date = :date
                                         and slot_hour = :hour and http_status = 200)""")
                .param("eva", eva)
                .param("date", date)
                .param("hour", hour)
                .query(Boolean.class)
                .single();
    }
}
