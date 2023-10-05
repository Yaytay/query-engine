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
public class ButtonTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testWithSize() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withSize(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withSize("md");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"size\":\"md\"}", baos.toString());    
  }

  @Test
  public void testWithLeftIcon() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withLeftIcon(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withLeftIcon("icon");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"leftIcon\":\"icon\"}", baos.toString());    
  }

  @Test
  public void testWithRightIcon() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withRightIcon(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withRightIcon("icon");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"rightIcon\":\"icon\"}", baos.toString());    
  }

  @Test
  public void testWithBlock() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withBlock(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withBlock(Boolean.TRUE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"block\":true}", baos.toString());    
  }

  @Test
  public void testWithAction() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withAction(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withAction(Button.ActionType.reset);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"action\":\"reset\"}", baos.toString());    
  }

  @Test
  public void testWithDisableOnInvalid() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withDisableOnInvalid(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withDisableOnInvalid(Boolean.FALSE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"disableOnInvalid\":false}", baos.toString());    
  }

  @Test
  public void testWithTheme() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withTheme(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Button field = new Button(generator)) {
        field.withTheme("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"button\",\"theme\":\"fred\"}", baos.toString());    
  }
  
}
