/*
 * Copyright (C) 2025 jtalbut
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

import inet.ipaddr.IPAddressString;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 *
 * @author jtalbut
 */
public class SourceStaticTest {
  
  private static final Logger logger = LoggerFactory.getLogger(SourceStaticTest.class);

  @Test
  public void testValidate() {
    RequestContext requestContext = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);   
    
    // Valid source
    SourceStatic source = SourceStatic.builder()
            .name("test")
            .types(Arrays.asList(
                    ColumnType.builder().column("id").type(DataType.Integer).build(),
                    ColumnType.builder().column("name").type(DataType.String).build()
            ))
            .rows(Arrays.asList(
                    Arrays.asList(1, "Alice"),
                    Arrays.asList(2, "Bob")
            ))
            .build();
    source.validate(pipelineContext);
    
    // Invalid type
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .type(SourceType.TEST)
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("id").type(DataType.Integer).build()
              ))
              .rows(Arrays.asList(
                      Arrays.asList(1)
              ))
              .build()
              .validate(pipelineContext);
    });
    
    // No types defined
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Collections.emptyList())
              .rows(Arrays.asList(
                      Arrays.asList(1)
              ))
              .build()
              .validate(pipelineContext);
    });
    
    // Column with no name
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("").type(DataType.Integer).build()
              ))
              .rows(Arrays.asList(
                      Arrays.asList(1)
              ))
              .build()
              .validate(pipelineContext);
    });
    
    // Column with no type
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("id").build()
              ))
              .rows(Arrays.asList(
                      Arrays.asList(1)
              ))
              .build()
              .validate(pipelineContext);
    });
    
    // No rows defined
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("id").type(DataType.Integer).build()
              ))
              .rows(Collections.emptyList())
              .build()
              .validate(pipelineContext);
    });
    
    // Row with wrong number of columns
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("id").type(DataType.Integer).build(),
                      ColumnType.builder().column("name").type(DataType.String).build()
              ))
              .rows(Arrays.asList(
                      Arrays.asList(1)  // Missing second column
              ))
              .build()
              .validate(pipelineContext);
    });
    
    // Row with value that cannot be converted to specified type
    assertThrows(IllegalArgumentException.class, () -> {
      SourceStatic.builder()
              .name("test")
              .types(Arrays.asList(
                      ColumnType.builder().column("id").type(DataType.Integer).build()
              ))
              .rows(Arrays.asList(
                      Arrays.asList("not_a_number")
              ))
              .build()
              .validate(pipelineContext);
    });
  }

  @Test
  public void testGetType() {
    SourceStatic source = SourceStatic.builder()
            .name("test")
            .types(Arrays.asList(
                    ColumnType.builder().column("id").type(DataType.Integer).build()
            ))
            .rows(Arrays.asList(
                    Arrays.asList(1)
            ))
            .build();
    assertEquals(SourceType.STATIC, source.getType());
  }

  @Test
  public void testGetName() {
    SourceStatic source = SourceStatic.builder()
            .name("test-name")
            .types(Arrays.asList(
                    ColumnType.builder().column("id").type(DataType.Integer).build()
            ))
            .rows(Arrays.asList(
                    Arrays.asList(1)
            ))
            .build();
    assertEquals("test-name", source.getName());
  }

  @Test
  public void testGetTypes() {
    List<ColumnType> types = Arrays.asList(
            ColumnType.builder().column("id").type(DataType.Integer).build(),
            ColumnType.builder().column("name").type(DataType.String).build()
    );
    SourceStatic source = SourceStatic.builder()
            .name("test")
            .types(types)
            .rows(Arrays.asList(
                    Arrays.asList(1, "Alice")
            ))
            .build();
    assertEquals(2, source.getTypes().size());
    assertEquals("id", source.getTypes().get(0).getColumn());
    assertEquals(DataType.Integer, source.getTypes().get(0).getType());
    assertEquals("name", source.getTypes().get(1).getColumn());
    assertEquals(DataType.String, source.getTypes().get(1).getType());
  }

  @Test
  public void testGetRows() {
    List<List<Object>> rows = Arrays.asList(
            Arrays.asList(1, "Alice"),
            Arrays.asList(2, "Bob"),
            Arrays.asList(3, "Charlie")
    );
    SourceStatic source = SourceStatic.builder()
            .name("test")
            .types(Arrays.asList(
                    ColumnType.builder().column("id").type(DataType.Integer).build(),
                    ColumnType.builder().column("name").type(DataType.String).build()
            ))
            .rows(rows)
            .build();
    assertEquals(3, source.getRows().size());
    assertEquals(Arrays.asList(1, "Alice"), source.getRows().get(0));
    assertEquals(Arrays.asList(2, "Bob"), source.getRows().get(1));
    assertEquals(Arrays.asList(3, "Charlie"), source.getRows().get(2));
  }

  @Test
  public void testBuilder() {
    SourceStatic source = SourceStatic.builder()
            .type(SourceType.STATIC)
            .name("test-builder")
            .types(Arrays.asList(
                    ColumnType.builder().column("col1").type(DataType.String).build()
            ))
            .rows(Arrays.asList(
                    Arrays.asList("value1")
            ))
            .build();
    
    assertEquals(SourceType.STATIC, source.getType());
    assertEquals("test-builder", source.getName());
    assertEquals(1, source.getTypes().size());
    assertEquals(1, source.getRows().size());
  }
}