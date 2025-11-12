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
package uk.co.spudsoft.query.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.DataType;

/**
 *
 * @author jtalbut
 */
public class PipelineInstanceTest {

  /**
   * Test of renderTemplate method, of class PipelineInstance.
   */
  @Test
  public void testRenderTemplate() {
    
    PipelineInstance instance = new PipelineInstance(null, null, null, null, null, null, null, null, null);
    assertNull(instance.renderTemplate("test", null));
    assertEquals("", instance.renderTemplate("test", ""));
    
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
      instance.renderTemplate("test", "$<£>");
    });
    assertThat(ex.getMessage(), containsString("£"));
  }
  
  @Test
  public void testRenderTemplateWithArgs() {
    PipelineInstance instance1 = new PipelineInstance(
            null, null, null,
            ImmutableMap.<String, ArgumentInstance>builder()
                    .put("port"
                            , new ArgumentInstance(
                                    Argument.builder()
                                            .name("port")
                                            .type(DataType.Integer)
                                            .build()
                                    , ImmutableList.<Comparable<?>>builder()
                                          .add(4)
                                          .build()
                            )
                    )
                    .build()
            , null, null, null, null, null);
    
    assertEquals("4", instance1.renderTemplate("test", "<args.port>"));
            
  }

}
