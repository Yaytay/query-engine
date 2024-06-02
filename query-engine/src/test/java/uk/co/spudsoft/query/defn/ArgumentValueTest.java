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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 *
 * @author jtalbut
 */
public class ArgumentValueTest {
  
  /**
   * Test of getId method, of class ArgumentValue.
   */
  @Test
  public void testGetId() {
    ArgumentValue instance = ArgumentValue.builder().build();
    assertNull(instance.getValue());
    instance = ArgumentValue.builder().value("id").build();
    assertEquals("id", instance.getValue());
  }

  /**
   * Test of getDisplay method, of class ArgumentValue.
   */
  @Test
  public void testGetDisplay() {
    ArgumentValue instance = ArgumentValue.builder().build();
    assertNull(instance.getLabel());
    instance = ArgumentValue.builder().label("d").build();
    assertEquals("d", instance.getLabel());
  }
}
