/*
 * Copyright (C) 2026 jtalbut
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
package uk.co.spudsoft.query.exec.dynamic;

import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive tests for {@link StringTemplateLocalDateTimeRenderer}.
 */
public class StringTemplateLocalDateTimeRendererTest {

  private final StringTemplateLocalDateTimeRenderer renderer = new StringTemplateLocalDateTimeRenderer();

  @Test
  public void testToString_nullObjectReturnsEmptyString() {
    assertEquals("", renderer.toString(null, null, null));
    assertEquals("", renderer.toString(null, "", null));
    assertEquals("", renderer.toString(null, "yyyy", Locale.UK));
  }

  @Test
  public void testToString_nullPatternReturnsLocalDateTimeToString() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 123_000_000);

    assertEquals(dt.toString(), renderer.toString(dt, null, null));
    assertEquals(dt.toString(), renderer.toString(dt, null, Locale.UK));
  }

  @Test
  public void testToString_emptyPatternReturnsLocalDateTimeToString() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    assertEquals(dt.toString(), renderer.toString(dt, "", null));
    assertEquals(dt.toString(), renderer.toString(dt, "", Locale.UK));
  }

  @Test
  public void testToString_basicNumericFormatting_localeNull() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    assertEquals("2026-02-16_0304", renderer.toString(dt, "yyyy-MM-dd_HHmm", null));
    assertEquals("20260216", renderer.toString(dt, "yyyyMMdd", null));
    assertEquals("03:04:05", renderer.toString(dt, "HH:mm:ss", null));
    assertEquals("2026-02-16T03:04", renderer.toString(dt, "yyyy-MM-dd'T'HH:mm", null));
  }

  @Test
  public void testToString_literalTextInPattern() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    // Single quotes denote literal text in DateTimeFormatter patterns
    assertEquals("at-03:04", renderer.toString(dt, "'at'-HH:mm", null));
    assertEquals("date=2026-02-16", renderer.toString(dt, "'date='yyyy-MM-dd", null));
  }

  @Test
  public void testToString_localeAffectsTextualMonthName() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    String en = renderer.toString(dt, "MMMM", Locale.UK);
    String fr = renderer.toString(dt, "MMMM", Locale.FRANCE);

    // We don't hardcode exact strings to avoid surprises across JDK locale data,
    // but we *do* assert the locale influences the output.
    assertNotNull(en);
    assertNotNull(fr);
    assertFalse(en.isBlank());
    assertFalse(fr.isBlank());
    assertNotEquals(en, fr, "Expected different month names when locale changes");
  }

  @Test
  public void testToString_invalidPatternThrowsIllegalArgumentException() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    assertThrows(IllegalArgumentException.class, () -> renderer.toString(dt, "not a pattern: [", null));
    assertThrows(IllegalArgumentException.class, () -> renderer.toString(dt, "yyyy-MM-dd 'unterminated", null));
  }

  @Test
  public void testIntegrationWithStringTemplateFormatOption() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    STGroup group = new STGroup();
    group.registerRenderer(LocalDateTime.class, new StringTemplateLocalDateTimeRenderer());

    ST st = new ST(group, "ConditionalArgument-<dt; format=\"yyyy-MM-dd_HHmm\">");
    st.add("dt", dt);

    assertEquals("ConditionalArgument-2026-02-16_0304", st.render());
  }

  @Test
  public void testIntegrationWithStringTemplateFormatOption_whenPatternIsEmptyFallsBackToToString() {
    LocalDateTime dt = LocalDateTime.of(2026, 2, 16, 3, 4, 5, 0);

    STGroup group = new STGroup();
    group.registerRenderer(LocalDateTime.class, new StringTemplateLocalDateTimeRenderer());

    ST st = new ST(group, "X=<dt; format=\"\">");
    st.add("dt", dt);

    assertEquals("X=" + dt.toString(), st.render());
  }

  @Test
  public void testIntegrationWithStringTemplate_whenValueIsNullRendersEmpty() {
    STGroup group = new STGroup();
    group.registerRenderer(LocalDateTime.class, new StringTemplateLocalDateTimeRenderer());

    ST st = new ST(group, "X=<dt; format=\"yyyy-MM-dd\">");
    st.add("dt", null);

    assertEquals("X=", st.render());
  }
}
