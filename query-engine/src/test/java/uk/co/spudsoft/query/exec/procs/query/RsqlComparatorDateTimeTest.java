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
package uk.co.spudsoft.query.exec.procs.query;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class RsqlComparatorDateTimeTest {

  private RsqlComparatorDateTime instance = new RsqlComparatorDateTime();
  
  private LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
  
  @Test
  public void testValidateType() {
    LocalDateTime valid = instance.validateType("field", LocalDateTime.now(ZoneOffset.UTC));
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.now());
    });
  }

  @Test
  public void testParseType() {
    assertEquals(LocalDateTime.of(1971, Month.MARCH, 4, 12, 34, 56), instance.parseType("field", "1971-03-04T12:34:56"));
    assertThrows(DateTimeParseException.class, () -> {
      instance.parseType("field", "x");
    });
  }

  @Test
  public void testEqual() {
    assertTrue(instance.equal(now, now));
    assertFalse(instance.equal(now, now.minusDays(1)));
  }

  @Test
  public void testNotEqual() {
    assertTrue(instance.notEqual(now, now.minusDays(1)));
    assertFalse(instance.notEqual(now, now));
  }

  @Test
  public void testGreaterThan() {
    assertTrue(instance.greaterThan(now, now.minusDays(1)));
    assertFalse(instance.greaterThan(now, now));
    assertFalse(instance.greaterThan(now, now.plusDays(1)));
  }

  @Test
  public void testGreaterThanOrEqual() {
    assertTrue(instance.greaterThanOrEqual(now, now.minusDays(1)));
    assertTrue(instance.greaterThanOrEqual(now, now));
    assertFalse(instance.greaterThanOrEqual(now, now.plusDays(1)));
  }

  @Test
  public void testLessThan() {
    assertTrue(instance.lessThan(now, now.plusDays(1)));
    assertFalse(instance.lessThan(now, now));
    assertFalse(instance.lessThan(now, now.minusDays(1)));
  }

  @Test
  public void testLessThanOrEqual() {
    assertTrue(instance.lessThanOrEqual(now, now.plusDays(1)));
    assertTrue(instance.lessThanOrEqual(now, now));
    assertFalse(instance.lessThanOrEqual(now, now.minusDays(1)));
  }

  @Test
  public void testIn() {
    assertTrue(instance.in(now, ImmutableSet.of(now.minusDays(1), now, now.plusDays(1))));
    assertFalse(instance.in(now, ImmutableSet.of(now.minusDays(1), now.plusDays(1))));
  }

  @Test
  public void testNotIn() {
    assertTrue(instance.notIn(now, ImmutableSet.of(now.minusDays(1), now.plusDays(1))));
    assertFalse(instance.notIn(now, ImmutableSet.of(now.minusDays(1), now, now.plusDays(1))));
  }
  
}
