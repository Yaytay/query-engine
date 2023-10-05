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
package uk.co.spudsoft.query.web.formio;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class TextAreaTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testWithRows() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (TextArea field = new TextArea(generator)) {
        field.withRows(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"textarea\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (TextArea field = new TextArea(generator)) {
        field.withRows(4);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"textarea\",\"rows\":4}", baos.toString());    
  }

  @Test
  public void testWithWysiwyg() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (TextArea field = new TextArea(generator)) {
        field.withWysiwyg(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"textarea\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (TextArea field = new TextArea(generator)) {
        field.withWysiwyg(Boolean.TRUE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"textarea\",\"wysiwyg\":true}", baos.toString());    
  }
  
}
