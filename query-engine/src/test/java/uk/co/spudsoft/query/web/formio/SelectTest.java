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
import uk.co.spudsoft.query.web.formio.Select.DataUrl;

/**
 *
 * @author jtalbut
 */
public class SelectTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testWithDataSrc() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withDataSrc(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withDataSrc(Select.DataSrcType.url);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"dataSrc\":\"url\"}", baos.toString());    
  }

  @Test
  @SuppressWarnings("try")
  public void testAddDataValues() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        try (Select.DataValues dv = field.addDataValues()) {
          try (ComponentArray ca = dv.addValues()) {
            field.addCompleteDataValue("label1", "value1");
            field.addCompleteDataValue("label2", "value2");
          }
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"dataSrc\":\"values\",\"data\":{\"values\":[{\"label\":\"label1\",\"value\":\"value1\"},{\"label\":\"label2\",\"value\":\"value2\"}]}}", baos.toString());    
  }

  @Test
  @SuppressWarnings("try")
  public void testAddDataUrl() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        try (DataUrl du = field.addDataUrl()) {
          du.withUrl("url");
          try (ComponentArray ca = du.addHeaders()) {
            field.addCompleteDataUrlHeader("key1", "value1");
            field.addCompleteDataUrlHeader("key2", "value2");
          }
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"dataSrc\":\"url\",\"data\":{\"url\":\"url\",\"headers\":[{\"key\":\"key1\",\"value\":\"value1\"},{\"key\":\"key2\",\"value\":\"value2\"}]}}", baos.toString());    
  }

  @Test
  public void testWithValueProperty() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withValueProperty(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withValueProperty("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"valueProperty\":\"fred\"}", baos.toString());    
  }

  @Test
  public void testWithRefreshOn() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withRefreshOn(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withRefreshOn("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"refreshOn\":\"fred\"}", baos.toString());    
  }

  @Test
  public void testWithFilter() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withFilter(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withFilter("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"filter\":\"fred\"}", baos.toString());    
  }

  @Test
  public void testWithAuthenticate() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withAuthenticate(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withAuthenticate(Boolean.FALSE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"authenticate\":false}", baos.toString());    
  }

  @Test
  public void testWithTemplate() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withTemplate(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Select field = new Select(generator)) {
        field.withTemplate("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"select\",\"template\":\"fred\"}", baos.toString());    
  }
  
}
