/*
 * Copyright (C) 2024 jtalbut
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ProcessorLookupFieldTest {
  
  @Test
  public void testValidate() {
    assertEquals(
            "No key field name provided for map"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookupField.builder()
                      .build().validate();
            }).getMessage()
    );
    assertEquals(
            "No value field name provided for map"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookupField.builder()
                      .keyField("key")
                      .build().validate();
            }).getMessage()
    );
    ProcessorLookupField.builder()
            .keyField("key")
            .valueField("value")
            .build().validate();
  }

  @Test
  public void testGetKeyField() {
    ProcessorLookupField instance = ProcessorLookupField.builder().build();
    assertNull(instance.getKeyField());
    instance = ProcessorLookupField.builder().keyField("key").build();
    assertEquals("key", instance.getKeyField());
  }

  @Test
  public void testGetValueField() {
    ProcessorLookupField instance = ProcessorLookupField.builder().build();
    assertNull(instance.getValueField());
    instance = ProcessorLookupField.builder().valueField("value").build();
    assertEquals("value", instance.getValueField());
  }

}
