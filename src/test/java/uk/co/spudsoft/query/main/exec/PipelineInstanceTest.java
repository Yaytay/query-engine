/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author jtalbut
 */
public class PipelineInstanceTest {
  
  @Test
  public void testGetArguments() {
    PipelineInstance instance = new PipelineInstance(null, null, null, null, null);
    assertNotNull(instance.getArguments());
    instance = new PipelineInstance(ImmutableMap.of(), null, null, null, null);
    assertNotNull(instance.getArguments());
    instance = new PipelineInstance(new HashMap<>(), null, null, null, null);
    assertNotNull(instance.getArguments());
    assertThat(instance.getProcessors(), instanceOf(ImmutableList.class));
  }

  @Test
  public void testGetProcessors() {
    PipelineInstance instance = new PipelineInstance(null, null, null, null, null);
    assertNotNull(instance.getProcessors());
    instance = new PipelineInstance(null, null, null, ImmutableList.of(), null);
    assertNotNull(instance.getProcessors());
    instance = new PipelineInstance(null, null, null, new ArrayList<>(), null);
    assertNotNull(instance.getProcessors());
    assertThat(instance.getProcessors(), instanceOf(ImmutableList.class));
  }

}
