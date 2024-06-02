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

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ProcessorLookupTest {
  
  @Test
  public void testValidate() {
    assertEquals(
            "Lookup source (lookupSource) pipeline not provided"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookup.builder()
                      .build().validate();
            }).getMessage()
    );
    
    assertEquals(
            "No fields provided to lookup (lookupFields)"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookup.builder()
                      .map(
                              SourcePipeline.builder()
                                      .source(
                                              SourceTest.builder()
                                                      .build()
                                      )
                                      .build()
                      )
                      .build().validate();
            }).getMessage()
    );
    
    assertEquals(
            "Lookup key field (lookupKeyField) not specified for lookup stream"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookup.builder()
                      .map(
                              SourcePipeline.builder()
                                      .source(
                                              SourceTest.builder()
                                                      .build()
                                      )
                                      .build()
                      )
                      .lookupFields(
                              Arrays.asList(
                                      ProcessorLookupField.builder().keyField("key").valueField("value").build()
                              )
                      )
                      .build().validate();
            }).getMessage()
    );

    assertEquals(
            "Lookup value field (lookupValueField) not specified for lookup stream"
            , assertThrows(IllegalArgumentException.class, () -> {
              ProcessorLookup.builder()
                      .map(
                              SourcePipeline.builder()
                                      .source(
                                              SourceTest.builder()
                                                      .build()
                                      )
                                      .build()
                      )
                      .lookupFields(
                              Arrays.asList(
                                      ProcessorLookupField.builder().keyField("key").valueField("value").build()
                              )
                      )
                      .lookupKeyField("key")
                      .build().validate();
            }).getMessage()
    );

    ProcessorLookup.builder()
            .map(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder()
                                            .build()
                            )
                            .build()
            )
            .lookupKeyField("key")
            .lookupValueField("value")
            .lookupFields(
                    Arrays.asList(
                            ProcessorLookupField.builder().keyField("key").valueField("value").build()
                    )
            )
            .build().validate();
  }

  @Test
  public void testGetType() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertEquals(ProcessorType.LOOKUP, instance.getType());
  }

  @Test
  public void testGetCondition() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertNull(instance.getCondition());
    instance = ProcessorLookup.builder().condition(new Condition("true")).build();
    assertEquals("true", instance.getCondition().getExpression());
  }

  @Test
  public void testGetId() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertNull(instance.getId());
    instance = ProcessorLookup.builder().id("id").build();
    assertEquals("id", instance.getId());
  }

  @Test
  public void testGetMap() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertNull(instance.getMap());
    instance = ProcessorLookup.builder().map(SourcePipeline.builder().build()).build();
    assertNotNull(instance.getMap());
  }

  @Test
  public void testGetLookupKeyField() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertNull(instance.getLookupKeyField());
    instance = ProcessorLookup.builder().lookupKeyField("key").build();
    assertEquals("key", instance.getLookupKeyField());
  }

  @Test
  public void testGetLookupValueField() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertNull(instance.getLookupValueField());
    instance = ProcessorLookup.builder().lookupValueField("value").build();
    assertEquals("value", instance.getLookupValueField());
  }

  @Test
  public void testGetLookupFields() {
    ProcessorLookup instance = ProcessorLookup.builder().build();
    assertEquals(0, instance.getLookupFields().size());
    instance = ProcessorLookup.builder().lookupFields(
            Arrays.asList(
                    ProcessorLookupField.builder().keyField("key").valueField("value").build()
            )
    ).build();
    assertEquals(1, instance.getLookupFields().size());
  }

}
