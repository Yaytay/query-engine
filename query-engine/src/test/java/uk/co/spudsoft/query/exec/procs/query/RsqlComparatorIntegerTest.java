/*
 * Copyright (C) 2024 njt
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class RsqlComparatorIntegerTest {

  private RsqlComparatorInteger instance = new RsqlComparatorInteger();
  
  private Integer base = 7;
  
  @Test
  public void testValidateType() {
    instance.validateType("field", base);
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }

  @Test
  public void testParseType() {
    assertEquals(9, instance.parseType("field", "9"));
    assertThrows(IllegalArgumentException.class, () -> {
      instance.parseType("field", "x");
    });
  }

  @Test
  public void testEqual() {
    assertTrue(instance.equal(base, base));
    assertFalse(instance.equal(base, base - 1));
  }

  @Test
  public void testNotEqual() {
    assertTrue(instance.notEqual(base, base - 1));
    assertFalse(instance.notEqual(base, base));
  }

  @Test
  public void testGreaterThan() {
    assertTrue(instance.greaterThan(base, base - 1));
    assertFalse(instance.greaterThan(base, base));
    assertFalse(instance.greaterThan(base, base + 1));
  }

  @Test
  public void testGreaterThanOrEqual() {
    assertTrue(instance.greaterThanOrEqual(base, base - 1));
    assertTrue(instance.greaterThanOrEqual(base, base));
    assertFalse(instance.greaterThanOrEqual(base, base + 1));
  }

  @Test
  public void testLessThan() {
    assertTrue(instance.lessThan(base, base + 1));
    assertFalse(instance.lessThan(base, base));
    assertFalse(instance.lessThan(base, base - 1));
  }

  @Test
  public void testLessThanOrEqual() {
    assertTrue(instance.lessThanOrEqual(base, base + 1));
    assertTrue(instance.lessThanOrEqual(base, base));
    assertFalse(instance.lessThanOrEqual(base, base - 1));
  }

  @Test
  public void testIn() {
    assertTrue(instance.in(base, ImmutableSet.of(base - 1, base, base + 1)));
    assertFalse(instance.in(base, ImmutableSet.of(base - 1, base + 1)));
  }

  @Test
  public void testNotIn() {
    assertTrue(instance.notIn(base, ImmutableSet.of(base - 1, base + 1)));
    assertFalse(instance.notIn(base, ImmutableSet.of(base - 1, base, base + 1)));
  }
  
}
