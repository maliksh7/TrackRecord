package io.github.maliksh7.trackrecord.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class IrisTimeTest {

    @Test
    void parsesSummerTimeAsCest() {
        assertThat(IrisTime.parse("2407151530"))
                .isEqualTo(OffsetDateTime.of(2024, 7, 15, 15, 30, 0, 0, ZoneOffset.ofHours(2)));
    }

    @Test
    void parsesWinterTimeAsCet() {
        assertThat(IrisTime.parse("2412240905"))
                .isEqualTo(OffsetDateTime.of(2024, 12, 24, 9, 5, 0, 0, ZoneOffset.ofHours(1)));
    }

    @Test
    void ambiguousAutumnHourResolvesToEarlierOffset() {
        // 27 Oct 2024: clocks went 03:00 CEST -> 02:00 CET, so 02:30 happened twice.
        assertThat(IrisTime.parse("2410270230").getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    void delayAcrossMidnightIsPositive() {
        var planned = IrisTime.parse("2410082355");
        var actual = IrisTime.parse("2410090012");
        assertThat(java.time.Duration.between(planned, actual).toMinutes()).isEqualTo(17);
    }

    @Test
    void blankIsNull() {
        assertThat(IrisTime.parse(null)).isNull();
        assertThat(IrisTime.parse(" ")).isNull();
    }

    @Test
    void malformedIsRejected() {
        assertThatThrownBy(() -> IrisTime.parse("2024-10-08")).isInstanceOf(IllegalArgumentException.class);
    }
}
