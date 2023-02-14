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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class PipelineTest {
  
  @Test
  public void testValidateNullFormats() {
    Pipeline instance = Pipeline.builder().source(SourceSql.builder().endpoint("end").query("select 1").build()).build();
    assertThrows(IllegalArgumentException.class, () -> {
      instance.validate();
    });
  }

  @Test
  public void testValidateNoFormats() {
    Pipeline instance = Pipeline.builder().source(SourceSql.builder().endpoint("end").query("select 1").build()).formats(Arrays.asList()).build();
    assertThrows(IllegalArgumentException.class, () -> {
      instance.validate();
    });
  }

  @Test
  public void testValidateArgumentDepends() {
    Pipeline instance1 = Pipeline.builder()
            .source(SourceTest.builder().rowCount(1).build())
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .arguments(
                    Arrays.asList(
                    )
            )
            .build();
    instance1.validate();

    Pipeline instance2 = Pipeline.builder()
            .source(SourceTest.builder().rowCount(1).build())
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .arguments(
                    Arrays.asList(
                            Argument.builder().name("arg1").type(ArgumentType.String).build()
                            , Argument.builder().name("arg2").type(ArgumentType.String).dependsUpon(Arrays.asList("arg1")).build()
                    )
            )
            .build();
    instance2.validate();

    Pipeline instance3 = Pipeline.builder()
            .source(SourceTest.builder().rowCount(1).build())
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .arguments(
                    Arrays.asList(
                            Argument.builder().name("arg1").type(ArgumentType.String).build()
                            , Argument.builder().name("arg").type(ArgumentType.String).dependsUpon(Arrays.asList("arg1")).build()
                    )
            )
            .build();
    instance3.validate();
  }

  @Test
  public void testGetArguments() {
    Pipeline instance = Pipeline.builder().build();
    assertTrue(instance.getArguments().isEmpty());
    Argument argument = Argument.builder().name("one").build();
    instance = Pipeline.builder().arguments(Arrays.asList(argument)).build();
    assertEquals(argument, instance.getArguments().get(0));
  }

  @Test
  public void testGetSourceEndpoints() {
    Pipeline instance = Pipeline.builder().build();
    assertTrue(instance.getSourceEndpoints().isEmpty());
    Endpoint endpoint = Endpoint.builder().build();
    instance = Pipeline.builder().sourceEndpoints(ImmutableMap.<String, Endpoint>builder().put("one", endpoint).build()).build();
    assertEquals(endpoint, instance.getSourceEndpoints().get("one"));
  }

  @Test
  public void testGetSource() {
    Pipeline instance = Pipeline.builder().build();
    assertNull(instance.getSource());
    Source source = SourceSql.builder().type(SourceType.SQL).build();
    instance = Pipeline.builder().source(source).build();
    assertEquals(source, instance.getSource());
  }

  @Test
  public void testGetFormats() {
    Pipeline instance = Pipeline.builder().build();
    assertNull(instance.getSource());
    Format format = FormatDelimited.builder().build();
    instance = Pipeline.builder().source(SourceSql.builder().endpoint("end").query("select 1").build()).formats(Arrays.asList(format)).build();
    assertEquals(format, instance.getFormats().get(0));
    instance.validate();
  }

  @Test
  public void testBadArgumentName() {
    assertThrows(IllegalArgumentException.class
            , () -> {
              Pipeline.builder()
                      .arguments(
                              Arrays.asList(
                                      Argument.builder().type(ArgumentType.Long).name("a b").build()
                              )
                      )
                      .build();
            });
  }

  
}