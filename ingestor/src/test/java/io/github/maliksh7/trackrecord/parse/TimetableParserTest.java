package io.github.maliksh7.trackrecord.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Stop;
import org.junit.jupiter.api.Test;

class TimetableParserTest {

    private final TimetableParser parser = new TimetableParser();

    @Test
    void parsesPlannedStops() {
        ParsedTimetable t = parser.parse(fixture("plan-sample.xml"));

        assertThat(t.stationName()).isEqualTo("Dresden Hbf");
        assertThat(t.stops()).hasSize(2);

        Stop re = t.stops().getFirst();
        assertThat(re.tripLabel().category()).isEqualTo("RE");
        assertThat(re.tripLabel().number()).isEqualTo("16104");
        assertThat(re.arrival().plannedTime()).isEqualTo(IrisTime.parse("2410081530"));
        assertThat(re.arrival().plannedPlatform()).isEqualTo("3");
        assertThat(re.arrival().line()).isEqualTo("50");
        assertThat(re.departure().plannedPath()).startsWith("Freital-Potschappel|");
        assertThat(re.arrival().hasChange()).isFalse();
    }

    @Test
    void originStopHasNoArrival() {
        Stop ice = parser.parse(fixture("plan-sample.xml")).stops().get(1);
        assertThat(ice.arrival()).isNull();
        assertThat(ice.departure().plannedTime()).isEqualTo(IrisTime.parse("2410081548"));
    }

    @Test
    void parsesChangesAndMessagesWithScope() {
        ParsedTimetable t = parser.parse(fixture("fchg-sample.xml"));
        Stop re = t.stops().getFirst();

        assertThat(t.eva()).isEqualTo("8010085");
        assertThat(re.tripLabel()).isNull();
        assertThat(re.arrival().changedTime()).isEqualTo(IrisTime.parse("2410081542"));
        assertThat(re.arrival().changedPlatform()).isEqualTo("4");
        assertThat(re.messages()).extracting(ParsedTimetable.Message::scope).containsExactly("s", "ar");
        assertThat(re.messages().get(1).code()).isEqualTo(43);
    }

    @Test
    void parsesCancellation() {
        Stop ice = parser.parse(fixture("fchg-sample.xml")).stops().get(1);
        assertThat(ice.departure().changedStatus()).isEqualTo("c");
        assertThat(ice.departure().hasChange()).isTrue();
    }

    @Test
    void rejectsDoctypeToPreventXxe() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE timetable [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <timetable station="&x;"/>""";
        assertThatThrownBy(() -> parser.parse(xxe)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnexpectedRoot() {
        assertThatThrownBy(() -> parser.parse("<error>quota exceeded</error>"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String fixture(String name) {
        try (InputStream in = TimetableParserTest.class.getResourceAsStream("/fixtures/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
