/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.main;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 *
 * @author jtalbut
 */
public class CacheConfigTest {
  
  @Test
  public void testGetMaxItems() {
    CacheConfig cc = new CacheConfig();
    assertEquals(100, cc.getMaxItems());
    cc.setMaxItems(17);
    assertEquals(17, cc.getMaxItems());
  }
  
  @Test
  public void testValidate() {
    CacheConfig cc = new CacheConfig();
    cc.validate("default");
    
    IllegalArgumentException ex;
    
    cc.setMaxItems(-1);
    ex = assertThrows(IllegalArgumentException.class, () -> {
      cc.validate("cache");
    });
    assertEquals("cache.maxItems configured with negative value (-1)", ex.getMessage());
    cc.setMaxItems(10);
    
    cc.setMaxDuration(Duration.ofDays(-1));
    ex = assertThrows(IllegalArgumentException.class, () -> {
      cc.validate("cache");
    });
    assertEquals("cache.maxDuration configured with negative value (PT-24H)", ex.getMessage());
    cc.setMaxDuration(Duration.ofDays(1));
    
    cc.setPurgePeriod(null);
    ex = assertThrows(IllegalArgumentException.class, () -> {
      cc.validate("cache");
    });
    assertEquals("cache.purgePeriod not configured", ex.getMessage());

    cc.setPurgePeriod(Duration.ofDays(-1));
    ex = assertThrows(IllegalArgumentException.class, () -> {
      cc.validate("cache");
    });
    assertEquals("cache.purgePeriod configured with value that is not positive (PT-24H)", ex.getMessage());
  }

}
