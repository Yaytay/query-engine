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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  
  @Test
  public void testEquals() {
    ArgumentValue av1 = ArgumentValue.builder().label("one").value("one").build();
    ArgumentValue av2 = ArgumentValue.builder().label("two").value("two").build();
    ArgumentValue av3 = ArgumentValue.builder().label("one").value("one").build();
    
    assertEquals(av1, av1);
    assertFalse(av1.equals(null));
    assertFalse(av1.equals("one"));
    assertFalse(av1.equals(av2));
    assertEquals(av1, av3);

    assertEquals(av1.hashCode(), av3.hashCode());
    assertFalse(av1.hashCode() == av2.hashCode());
  }
}
