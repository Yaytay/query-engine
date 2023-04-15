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
package uk.co.spudsoft.query.logging;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 *
 * @author jtalbut
 */
public class LogbackOptionsTest {
  
  @Test
  public void testGetConfigFile() {
    LogbackOptions instance = new LogbackOptions();
    assertEquals(null, instance.getConfigFile());
    instance.setConfigFile("bob");
    assertEquals("bob", instance.getConfigFile());
  }

  @Test
  public void testIsJsonFormat() {
    LogbackOptions instance = new LogbackOptions();
    assertFalse(instance.isJsonFormat());
    instance.setJsonFormat(true);    
    assertTrue(instance.isJsonFormat());
  }

  @Test
  public void testGetLevel() {
    LogbackOptions instance = new LogbackOptions();
    assertNull(instance.getLevel());
    Map<String, Level> levels = new HashMap<>();
    levels.put("bob.fred", Level.DEBUG);
    levels.put("bob_carol", Level.TRACE);
    instance.setLevel(levels);
    assertEquals(Level.DEBUG, instance.getLevel().get("bob.fred"));
    assertEquals(Level.TRACE, instance.getLevel().get("bob.carol"));
  }
  
}
