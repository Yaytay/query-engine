/*
 * Copyright (C) 2023 njt
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
package uk.co.spudsoft.query.defn;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class ConcurrencyRuleTest {
  
  @Test
  public void testValidate() {
    ConcurrencyRule instance1 = ConcurrencyRule.builder().build();
    assertThrows(IllegalArgumentException.class, () -> { instance1.validate(); });
    ConcurrencyRule instance2 = ConcurrencyRule.builder()
            .scope(Arrays.asList(ConcurrencyScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance2.validate(); });
    ConcurrencyRule instance3 = ConcurrencyRule.builder()
            .timeLimit(Duration.of(1, ChronoUnit.MILLIS))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance3.validate(); });
    ConcurrencyRule instance4 = ConcurrencyRule.builder()
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .scope(Arrays.asList(ConcurrencyScopeType.path))
            .build();
    instance4.validate();
    ConcurrencyRule instance5 = ConcurrencyRule.builder()
            .timeLimit(Duration.of(-1, ChronoUnit.HOURS))
            .scope(Arrays.asList(ConcurrencyScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance5.validate(); });
    ConcurrencyRule instance6 = ConcurrencyRule.builder()
            .timeLimit(Duration.ZERO)
            .scope(Arrays.asList(ConcurrencyScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance6.validate(); });
  }

  @Test
  public void testGetScope() {
    ConcurrencyRule instance = ConcurrencyRule.builder().build();
    assertThat(instance.getScope(), hasSize(0));
    instance = ConcurrencyRule.builder().scope(Arrays.asList(ConcurrencyScopeType.clientip)).build();
    assertEquals(ConcurrencyScopeType.clientip, instance.getScope().get(0));
  }

  @Test
  public void testGetTimeLimit() {
    ConcurrencyRule instance = ConcurrencyRule.builder().build();
    assertNull(instance.getTimeLimit());
    instance = ConcurrencyRule.builder().timeLimit(Duration.of(1, ChronoUnit.DAYS)).build();
    assertEquals(24, instance.getTimeLimit().toHours());
  }
  
}
