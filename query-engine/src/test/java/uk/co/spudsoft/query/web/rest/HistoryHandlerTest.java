/*
 * Copyright (C) 2024 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.web.rest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.hamcrest.TypeSafeMatcher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class HistoryHandlerTest {

  @Test
  public void testBoundInt() {
    assertEquals(3, HistoryHandler.boundInt(3, 0, 0, 10));
    assertEquals(3, HistoryHandler.boundInt(null, 3, 0, 10));
    assertEquals(3, HistoryHandler.boundInt(-1, 7, 3, 10));
    assertEquals(10, HistoryHandler.boundInt(13, 7, 3, 10));
  }

  @Test
  public void testParseIso8601_NullOrEmpty() {
    assertThat(HistoryHandler.parseIso8601("testParseIso8601_NullOrEmpty", null), is(nullValue()));
    assertThat(HistoryHandler.parseIso8601("testParseIso8601_NullOrEmpty", ""), is(nullValue()));
    assertThat(HistoryHandler.parseIso8601("testParseIso8601_NullOrEmpty", "   "), is(nullValue()));
  }

  @Test
  public void testParseIso8601_SpecificDateTimeUtc() {
    String input = "2026-01-07T12:00:00Z";
    LocalDateTime result = HistoryHandler.parseIso8601("testParseIso8601_SpecificDateTimeUtc", input);
    assertThat(result, is(LocalDateTime.of(2026, 1, 7, 12, 0, 0)));
  }

  @Test
  public void testParseIso8601_SpecificDateTimeWithOffset() {
    // 10:00 at +02:00 should be 08:00 UTC
    String input = "2026-01-07T10:00:00+02:00";
    LocalDateTime result = HistoryHandler.parseIso8601("testParseIso8601_SpecificDateTimeWithOffset", input);
    assertThat(result, is(LocalDateTime.of(2026, 1, 7, 8, 0, 0)));
  }

  @Test
  public void testParseIso8601_SpecificDateTimeNoZone() {
    String input = "2026-01-07T15:30:00";
    LocalDateTime result = HistoryHandler.parseIso8601("testParseIso8601_SpecificDateTimeNoZone", input);
    assertThat(result, is(LocalDateTime.of(2026, 1, 7, 15, 30, 0)));
  }

  @Test
  public void testParseIso8601_DurationPositive() {
    String input = "PT1H";
    LocalDateTime expected = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
    LocalDateTime result = HistoryHandler.parseIso8601("testParseIso8601_DurationPositive", input);

    assertThat(result, isWithinSecondsOf(expected, 2));
  }

  @Test
  public void testParseIso8601_DurationNegative() {
    String input = "-P1D";
    LocalDateTime expected = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);
    LocalDateTime result = HistoryHandler.parseIso8601("testParseIso8601_DurationNegative", input);

    assertThat(result, isWithinSecondsOf(expected, 2));
  }

  @Test
  public void testParseIso8601_InvalidString_ThrowsException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()
            -> HistoryHandler.parseIso8601("testParseIso8601_InvalidString_ThrowsException", "not-a-date")
    );
    assertThat(ex.getMessage(), containsString("testParseIso8601_InvalidString_ThrowsException"));
    assertThat(ex.getMessage(), containsString("not-a-date"));
  }

  /**
   * Custom Hamcrest matcher to handle execution delays in duration tests.
   */
  private static Matcher<LocalDateTime> isWithinSecondsOf(LocalDateTime expected, long seconds) {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(LocalDateTime actual) {
        long diff = Math.abs(ChronoUnit.SECONDS.between(expected, actual));
        return diff <= seconds;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a LocalDateTime within ").appendValue(seconds)
                .appendText(" seconds of ").appendValue(expected);
      }
    };
  }
}
