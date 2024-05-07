/*
 * Copyright (C) 2023 jtalbut
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
 * @author jtalbut
 */
public class RateLimitRuleTest {
  
  @Test
  public void testValidate() {
    RateLimitRule instance1 = RateLimitRule.builder()
            .byteLimit("17M")
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    instance1.validate();
    
    RateLimitRule instance2 = RateLimitRule.builder()
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .concurrencyLimit(-2)
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance2.validate(); });
    
    RateLimitRule instance3 = RateLimitRule.builder()
            .byteLimit("red")
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance3.validate(); });
    
    RateLimitRule instance4 = RateLimitRule.builder()
            .byteLimit("-7")
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance4.validate(); });
    
    RateLimitRule instance5 = RateLimitRule.builder()
            .byteLimit("17k")
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance5.validate(); });
    
    RateLimitRule instance6 = RateLimitRule.builder()
            .byteLimit("17")
            .timeLimit(Duration.of(0, ChronoUnit.MICROS))
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance6.validate(); });
    
    RateLimitRule instance7 = RateLimitRule.builder()
            .byteLimit("17")
            .timeLimit(Duration.of(-1, ChronoUnit.MICROS))
            .scope(Arrays.asList(RateLimitScopeType.path))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance7.validate(); });
    
    RateLimitRule instance8 = RateLimitRule.builder()
            .byteLimit("17")
            .timeLimit(Duration.of(1, ChronoUnit.MICROS))
            .build();
    assertThrows(IllegalArgumentException.class, () -> { instance8.validate(); });
    
  }

  @Test
  public void testGetScope() {
    RateLimitRule instance = RateLimitRule.builder().build();
    assertThat(instance.getScope(), hasSize(0));
    instance = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.clientip)).build();
    assertEquals(RateLimitScopeType.clientip, instance.getScope().get(0));
  }

  @Test
  public void testGetTimeLimit() {
    RateLimitRule instance = RateLimitRule.builder().build();
    assertNull(instance.getTimeLimit());
    instance = RateLimitRule.builder().timeLimit(Duration.of(1, ChronoUnit.DAYS)).build();
    assertEquals(24, instance.getTimeLimit().toHours());
  }

  @Test
  public void testGetByteLimit() {
    RateLimitRule instance = RateLimitRule.builder().build();
    assertNull(instance.getByteLimit());
    instance = RateLimitRule.builder().byteLimit("13").build();
    assertEquals("13", instance.getByteLimit());
    instance = RateLimitRule.builder().byteLimit("13").build();
    assertEquals(13L, instance.getParsedByteLimit());
    instance = RateLimitRule.builder().byteLimit("13G").build();
    assertEquals(13000000000L, instance.getParsedByteLimit());
    instance = RateLimitRule.builder().byteLimit("13M").build();
    assertEquals(13000000L, instance.getParsedByteLimit());
    instance = RateLimitRule.builder().byteLimit("13k").build();
    assertEquals(13000L, instance.getParsedByteLimit());
  }
  
}
