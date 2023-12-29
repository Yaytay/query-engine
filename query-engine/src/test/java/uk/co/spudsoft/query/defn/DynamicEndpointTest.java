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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class DynamicEndpointTest {
  
  /**
   * Test of validate method, of class DynamicEndpoint.
   */
  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      DynamicEndpoint.builder().build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      DynamicEndpoint.builder().input(
              SourcePipeline.builder()
                      .source(SourceTest.builder().name("test").build())                      
                      .build()
      ).build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      DynamicEndpoint.builder()
              .input(
                      SourcePipeline.builder()
                              .source(SourceTest.builder().name("test").build())
                              .build()
              )
              .keyField("key")
              .urlField(null)
              .urlTemplateField("")
              .build()
              .validate();
    });
    DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .urlField("url")
            .build().validate();    
    DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .keyField("key")
            .urlTemplateField("url")
            .build().validate();    
  }
  
  @Test
  public void testKey() {
    DynamicEndpoint de = DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .urlField("url")
            .typeField("type")
            .build()
            ;
    assertEquals("key", de.getKey());
  }
  
  @Test
  public void testSecretField() {
    DynamicEndpoint de = DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .secretField("secret")
            .typeField("type")
            .build()
            ;
    assertEquals("secret", de.getSecretField());
  }
  
  @Test
  public void testUsernameField() {
    DynamicEndpoint de = DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .usernameField("username")
            .typeField("type")
            .build()
            ;
    assertEquals("username", de.getUsernameField());
  }
  
  @Test
  public void testPasswordField() {
    DynamicEndpoint de = DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .passwordField("password")
            .typeField("type")
            .build()
            ;
    assertEquals("password", de.getPasswordField());
  }
  
  @Test
  public void testConditionField() {
    DynamicEndpoint de = DynamicEndpoint.builder()
            .input(
                    SourcePipeline.builder()
                            .source(SourceTest.builder().name("test").build())
                            .build()
                    )
            .key("key")
            .conditionField("condition")
            .typeField("type")
            .build()
            ;
    assertEquals("condition", de.getConditionField());
  }
}
