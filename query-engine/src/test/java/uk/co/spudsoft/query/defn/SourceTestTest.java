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
package uk.co.spudsoft.query.defn;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class SourceTestTest {
  
  @Test
  public void testGetType() {
    SourceTest instance = SourceTest.builder().build();
    assertEquals(SourceType.TEST, instance.getType());
  }

  @Test
  public void testSetType() {
    SourceTest instance = SourceTest.builder().type(SourceType.TEST).build();
    assertEquals(SourceType.TEST, instance.getType());
    try {
      SourceTest.builder().type(SourceType.HTTP).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetRowCount() {
    SourceTest instance = SourceTest.builder().rowCount(17).build();
    assertEquals(17, instance.getRowCount());
  }

  @Test
  public void testGetName() {
    SourceTest instance = SourceTest.builder().name("name").build();
    assertEquals("name", instance.getName());
  }
  
  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      SourceTest.builder().build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      SourceTest.builder().name("name").rowCount(-1).build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      SourceTest.builder().name("name").delayMs(-1).build().validate();
    });
    
  }
  
}
