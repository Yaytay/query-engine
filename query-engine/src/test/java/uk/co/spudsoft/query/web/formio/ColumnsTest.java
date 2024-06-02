/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web.formio;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.web.formio.Columns.Column;

/**
 *
 * @author jtalbut
 */
public class ColumnsTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  @SuppressWarnings("try")
  public void testAddColumn() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Columns field = new Columns(generator)) {
      }
    }
    baos.close();
    assertEquals("{\"type\":\"columns\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Columns field = new Columns(generator)) {
        try (ComponentArray array = field.addColumns()) {
          try (Column column = new Columns.Column(generator)) {
            column.withOffset(0);
            column.withPull(0);
            column.withPush(0);
            column.withSize("md");
            column.withWidth(200);
          }
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"columns\",\"columns\":[{\"offset\":0,\"pull\":0,\"push\":0,\"size\":\"md\",\"width\":200}]}", baos.toString());    
  }
  
}
