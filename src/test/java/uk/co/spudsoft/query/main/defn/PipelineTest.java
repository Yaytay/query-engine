/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class PipelineTest {
  
  @Test
  public void testValidate() {
    Pipeline instance = Pipeline.builder().build();
    instance.validate();
  }

  @Test
  public void testGetArguments() {
    Pipeline instance = Pipeline.builder().build();
    assertTrue(instance.getArguments().isEmpty());
    Argument argument = Argument.builder().build();
    instance = Pipeline.builder().arguments(ImmutableMap.<String, Argument>builder().put("one", argument).build()).build();
    assertEquals(argument, instance.getArguments().get("one"));
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
  public void testGetDestination() {
    Pipeline instance = Pipeline.builder().build();
    assertNull(instance.getSource());
    Destination destination = DestinationLogger.builder().build();
    instance = Pipeline.builder().destination(destination).build();
    assertEquals(destination, instance.getDestination());
  }

}
