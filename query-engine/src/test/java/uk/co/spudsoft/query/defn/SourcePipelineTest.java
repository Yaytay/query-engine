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

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class SourcePipelineTest {
  
  private static final Logger logger = LoggerFactory.getLogger(SourcePipelineTest.class);

  @Test
  public void testValidateNullSource() {
    SourcePipeline pipeline = SourcePipeline.builder()
            .build();
    assertThrows(IllegalArgumentException.class
            ,
            () -> {
              pipeline.validate();
            }
    );
  }
  
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
            .source(SourceTest.builder().build())
            .processors(null)
            .build();
    assertNotNull(pipeline.getSource());
    assertNotNull(pipeline.getProcessors());
    pipeline.validate();
  }
}
