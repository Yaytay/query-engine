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

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorMapTest {
  
  @Test
  public void testGetId() {
    ProcessorMap instance = ProcessorMap.builder().id("id").build();
    assertEquals("id", instance.getId());
  }

  @Test
  public void testSetType() {
    ProcessorMap instance = ProcessorMap.builder().build();
    assertEquals(ProcessorType.MAP, instance.getType());
    instance = ProcessorMap.builder().type(ProcessorType.MAP).build();
    assertEquals(ProcessorType.MAP, instance.getType());
    try {
      ProcessorMap.builder().type(ProcessorType.SCRIPT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetRelabel() {
    ProcessorMap instance = ProcessorMap.builder().relabels(Arrays.asList()).build();
    assertEquals(0, instance.getRelabels().size());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMap.builder().relabels(Arrays.asList()).build().validate();
    }, "Zero relabels provided");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMap.builder().relabels(Arrays.asList(ProcessorMapLabel.builder().build())).build().validate();
    }, "Zero relabels provided");
    ProcessorMap.builder().relabels(Arrays.asList(ProcessorMapLabel.builder().sourceLabel("source").build())).build().validate();
    ProcessorMap.builder().relabels(Arrays.asList(ProcessorMapLabel.builder().sourceLabel("source").newLabel("new").build())).build().validate();
  }
  
}
