/*
 * Copyright (C) 2023 njt
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
package uk.co.spudsoft.query.main;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.Processor;

/**
 *
 * @author njt
 */
public class OpenApiModelConverterTest {

  public OpenApiModelConverterTest() {
  }

  /**
   * Test of resolve method, of class OpenApiModelConverter.
   */
  @Test
  public void testResolve() {
    @SuppressWarnings("unchecked")
    Iterator<ModelConverter> chain = mock(Iterator.class);
    when(chain.hasNext()).thenReturn(false);

    OpenApiModelConverter oamc = new OpenApiModelConverter();
    assertNull(oamc.resolve(null, null, chain));
  }

  /**
   * Test of isOpenapi31 method, of class OpenApiModelConverter.
   */
  @Test
  public void testIsOpenapi31() {
    OpenApiModelConverter oamc = new OpenApiModelConverter();
    assertTrue(oamc.isOpenapi31());
  }

  /**
   * Test of removeEmptyProperty method, of class OpenApiModelConverter.
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void testRemoveEmptyProperty() {

    OpenApiModelConverter.removeEmptyProperty(null);

    Schema schema = mock(Schema.class);
    OpenApiModelConverter.removeEmptyProperty(schema);

    Map<String, Object> map = new HashMap<>();
    when(schema.getProperties()).thenReturn(map);

    OpenApiModelConverter.removeEmptyProperty(schema);
    map.put("empty", "yes");
    map.put("other", "yes");
    OpenApiModelConverter.removeEmptyProperty(schema);
    assertEquals(1, map.size());

  }

  /**
   * Test of removeEmptyProperty method, of class OpenApiModelConverter.
   */
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testConvertDuration() {

    OpenApiModelConverter.convertDuration(null);;

    Schema schema = mock(Schema.class);
    OpenApiModelConverter.convertDuration(schema);

    verify(schema, times(1)).setTypes(ImmutableSet.builder().add("string").build());
    verify(schema, times(1)).setProperties(null);
    verify(schema, times(1)).setMaxLength(40);
    verify(schema, times(1)).setPattern("^P(?!$)(\\\\d+Y)?(\\\\d+M)?(\\\\d+W)?(\\\\d+D)?(T(?=\\\\d)(\\\\d+H)?(\\\\d+M)?(\\\\d+S)?)?$");
  }

  private static class SourcePipelineArraySchema {

    private final ImmutableList<Processor> processors;

    public SourcePipelineArraySchema(ImmutableList<Processor> processors) {
      this.processors = processors;
    }

    @ArraySchema(
            items = @io.swagger.v3.oas.annotations.media.Schema(implementation = Processor.class)
            , arraySchema = @io.swagger.v3.oas.annotations.media.Schema(type = "array", description = "It's the processors")
            , minItems = 0
    )
    public List<Processor> getProcessors() {
      return processors;
    }

  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testFixArrrayPropertyDescriptionsArraySchema() {
    
    JsonSchema propSchema = new JsonSchema();
    propSchema.name("processors");
    propSchema.minItems(0);
    JsonSchema schema = new JsonSchema();
    schema.setProperties(ImmutableMap.<String,Schema>builder()
            .put("processors", propSchema)
            .build());
    OpenApiModelConverter.fixArrrayPropertyDescriptions(SourcePipelineArraySchema.class, schema);
    String processorsString = schema.getProperties().get("processors").toString();
    assertThat(processorsString, containsString("type: [array]"));
    assertThat(processorsString, containsString("description: It's the processors"));
    assertThat(processorsString, containsString("minItems: 0"));
  }

  private static class SourcePipelineItemSchema {

    private final ImmutableList<Processor> processors;

    public SourcePipelineItemSchema(ImmutableList<Processor> processors) {
      this.processors = processors;
    }

    @ArraySchema(
            items = @io.swagger.v3.oas.annotations.media.Schema(implementation = Processor.class, description = "It's the processors")
            , arraySchema = @io.swagger.v3.oas.annotations.media.Schema(type = "array")
            , minItems = 0
    )
    public List<Processor> getProcessors() {
      return processors;
    }

  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testFixArrrayPropertyDescriptionsItemSchema() {
    
    JsonSchema propSchema = new JsonSchema();
    propSchema.name("processors");
    propSchema.minItems(0);
    JsonSchema schema = new JsonSchema();
    schema.setProperties(ImmutableMap.<String,Schema>builder()
            .put("processors", propSchema)
            .build());
    OpenApiModelConverter.fixArrrayPropertyDescriptions(SourcePipelineItemSchema.class, schema);
    String processorsString = schema.getProperties().get("processors").toString();
    assertThat(processorsString, containsString("type: [array]"));
    assertThat(processorsString, containsString("description: It's the processors"));
    assertThat(processorsString, containsString("minItems: 0"));
  }

  private static class SourcePipelineNoSchema {

    private final ImmutableList<Processor> processors;

    public SourcePipelineNoSchema(ImmutableList<Processor> processors) {
      this.processors = processors;
    }

    @ArraySchema(
            minItems = 0
    )
    public List<Processor> getProcessors() {
      return processors;
    }

  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testFixArrrayPropertyDescriptionsNoSchema() {
    
    JsonSchema propSchema = new JsonSchema();
    propSchema.name("processors");
    propSchema.minItems(0);
    JsonSchema schema = new JsonSchema();
    schema.setProperties(ImmutableMap.<String,Schema>builder()
            .put("processors", propSchema)
            .build());
    OpenApiModelConverter.fixArrrayPropertyDescriptions(SourcePipelineNoSchema.class, schema);
    String processorsString = schema.getProperties().get("processors").toString();
    assertThat(processorsString, containsString("type: [array]"));
    assertThat(processorsString, containsString("description: null"));
    assertThat(processorsString, containsString("minItems: 0"));
  }

  private static class SourcePipelineItemsRequired {

    private final ImmutableList<Processor> processors;

    public SourcePipelineItemsRequired(ImmutableList<Processor> processors) {
      this.processors = processors;
    }

    @ArraySchema(
            items = @io.swagger.v3.oas.annotations.media.Schema(implementation = Processor.class, description = "It's the processors")
            , arraySchema = @io.swagger.v3.oas.annotations.media.Schema(type = "array", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
            , minItems = 1
    )
    public List<Processor> getProcessors() {
      return processors;
    }

  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testFixArrrayPropertyDescriptionsRequired() {
    
    JsonSchema propSchema = new JsonSchema();
    propSchema.name("processors");
    propSchema.minItems(0);
    JsonSchema schema = new JsonSchema();
    schema.setProperties(ImmutableMap.<String,Schema>builder()
            .put("processors", propSchema)
            .build());
    OpenApiModelConverter.fixArrrayPropertyDescriptions(SourcePipelineItemsRequired.class, schema);
    String processorsString = schema.getProperties().get("processors").toString();
    assertThat(processorsString, containsString("type: [array]"));
    assertThat(processorsString, containsString("description: It's the processors"));
    String parentString = schema.toString();
    assertThat(processorsString, containsString("minItems: 0"));
    assertThat(parentString, containsString("required: [processors]"));
  }

}
