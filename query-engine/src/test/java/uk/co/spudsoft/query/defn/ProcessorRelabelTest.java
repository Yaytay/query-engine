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
public class ProcessorRelabelTest {
  
  @Test
  public void testGetType() {
    ProcessorRelabel instance = ProcessorRelabel.builder().build();
    assertEquals(ProcessorType.RELABEL, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorRelabel instance = ProcessorRelabel.builder().type(ProcessorType.RELABEL).build();
    assertEquals(ProcessorType.RELABEL, instance.getType());
    try {
      ProcessorRelabel.builder().type(ProcessorType.SCRIPT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetRelabel() {
    ProcessorRelabel instance = ProcessorRelabel.builder().relabels(Arrays.asList()).build();
    assertEquals(0, instance.getRelabels().size());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorRelabel.builder().relabels(Arrays.asList()).build().validate();
    }, "Zero relabels provided");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorRelabel.builder().relabels(Arrays.asList(ProcessorRelabelLabel.builder().build())).build().validate();
    }, "Zero relabels provided");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorRelabel.builder().relabels(Arrays.asList(ProcessorRelabelLabel.builder().sourceLabel("source").build())).build().validate();
    }, "Zero relabels provided");
    ProcessorRelabel.builder().relabels(Arrays.asList(ProcessorRelabelLabel.builder().sourceLabel("source").newLabel("new").build())).build().validate();
  }
  
}
