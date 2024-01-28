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
package uk.co.spudsoft.query.defn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class ProcessorQueryTest {
  
  @Test
  public void testGetType() {
    ProcessorQuery instance = ProcessorQuery.builder().build();
    assertEquals(ProcessorType.QUERY, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorQuery instance = ProcessorQuery.builder().type(ProcessorType.QUERY).build();
    assertEquals(ProcessorType.QUERY, instance.getType());
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorQuery.builder().type(ProcessorType.SCRIPT).build();
    });
  }

  @Test
  public void testGetQuery() {
    ProcessorQuery instance = ProcessorQuery.builder().expression("x==4").build();
    assertEquals("x==4", instance.getExpression());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorQuery.builder().expression("bob").build().validate();
    }, "Zero limit provided");
    ProcessorQuery.builder().expression("x==7").build().validate();
  }
  
  
}
