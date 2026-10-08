package io.github.maliksh7.trackrecord.parse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * IRIS timestamps are "yyMMddHHmm" strings in German local time with no offset, e.g. "2410081530".
 *
 * <p>Known trap: during the autumn DST switch (03:00 -> 02:00) every time between 02:00 and 02:59
 * occurs twice and the string alone cannot tell which one is meant. We resolve to the earlier offset
 * (CEST), which is what {@link java.time.ZonedDateTime#of} does by default; record any anomalies this
 * causes in docs/03-data-validation.md.
 */
public final class IrisTime {

    public static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmm");

    private IrisTime() {
    }

    /** Returns null for null/blank input; throws {@link IllegalArgumentException} on malformed input. */
    public static OffsetDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORMAT).atZone(BERLIN).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Not an IRIS timestamp (yyMMddHHmm): '" + value + "'", e);
        }
    }
}
