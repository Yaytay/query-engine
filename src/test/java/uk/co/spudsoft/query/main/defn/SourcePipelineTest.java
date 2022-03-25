/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author jtalbut
 */
public class SourcePipelineTest {

  @Test
  public void testBuilder() {
    SourcePipeline pipeline = SourcePipeline.builder()
            .source(SourceTest.builder().rowCount(4).build())
            .processors(new ArrayList<>())
            .build();
    assertEquals(SourceType.TEST, pipeline.getSource().getType());
    assertEquals(4, ((SourceTest) pipeline.getSource()).getRowCount());
    assertNotNull(pipeline.getProcessors());
    pipeline.validate();
    
    pipeline = SourcePipeline.builder()
            .source(null)
            .processors(null)
            .build();
    assertNull(pipeline.getSource());
    assertNotNull(pipeline.getProcessors());
    pipeline.validate();
  }
  
}
