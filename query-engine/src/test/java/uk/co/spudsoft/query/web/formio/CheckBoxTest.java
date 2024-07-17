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


/**
 *
 * @author jtalbut
 */
public class CheckBoxTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testWithClearOnHide() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withClearOnHide(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"clearOnHide\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withClearOnHide(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"clearOnHide\":false}", baos.toString());    
  }
  
  @Test
  public void testWithCustomClass() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withCustomClass(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withCustomClass("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"customClass\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithDefaultValue() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withDefaultValue(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withDefaultValue("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"defaultValue\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithDescription() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withDescription(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withDescription("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"description\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithHidden() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withHidden(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"hidden\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withHidden(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"hidden\":false}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withHidden(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    
  }
  
  @Test
  public void testWithInput() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withInput(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"input\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withInput(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"input\":false}", baos.toString());    
  }
  
  @Test
  public void testWithKey() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withKey(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withKey("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"key\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithLabel() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withLabel(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withLabel("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"label\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithMultiple() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withMultiple(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"multiple\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withMultiple(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"multiple\":false}", baos.toString());    
  }
  
  @Test
  public void testWithPersistent() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPersistent(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"persistent\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPersistent(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"persistent\":false}", baos.toString());    
  }
  
  @Test
  public void testWithPlaceholder() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPlaceholder(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPlaceholder("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"placeholder\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithPrefix() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPrefix(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withPrefix("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"prefix\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithProtect() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withProtect(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"protect\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withProtect(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"protect\":false}", baos.toString());    
  }
  
  @Test
  public void testWithSuffix() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withSuffix(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withSuffix("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"suffix\":\"fred\"}", baos.toString());    
  }
  
  @Test
  public void testWithTableView() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withTableView(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"tableView\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withTableView(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"tableView\":false}", baos.toString());    
  }
  
  @Test
  public void testWithUnique() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withUnique(true);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"unique\":true}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        field.withUnique(false);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"unique\":false}", baos.toString());    
  }
  
  @Test
  public void testAddErrors() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        try (Errors errors = field.addErrors()) {
          errors.withCustom("Custom");
          errors.withInvalidDate("Invalid Date");
          errors.withInvalidEmail("Invalid Email");
          errors.withMax("Max");
          errors.withMin("Min");
          errors.withMaxLength("Max Length");
          errors.withMinLength("Min Length");
          errors.withPattern("Pattern");
          errors.withRequired("Required");
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"errors\":{\"custom\":\"Custom\",\"invalid_date\":\"Invalid Date\",\"invalid_email\":\"Invalid Email\",\"max\":\"Max\",\"min\":\"Min\",\"maxLength\":\"Max Length\",\"minLength\":\"Min Length\",\"pattern\":\"Pattern\",\"required\":\"Required\"}}", baos.toString());    
  }
  
  @Test
  public void testAddValidation() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        try (Validation validation = field.addValidate()) {
          validation.withCustom("custom");
          validation.withMaxLength(12);
          validation.withMinLength(0);
          validation.withPattern(".*");
          validation.withRequired(Boolean.FALSE);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"validate\":{\"custom\":\"custom\",\"maxLength\":12,\"minLength\":0,\"pattern\":\".*\",\"required\":false}}", baos.toString());    
  }
  
  @Test
  public void testAddConditional() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (CheckBox field = new CheckBox(generator)) {
        try (Conditional conditional = field.addConditional()) {
          conditional.withEq("eq");
          conditional.withJson("json");
          conditional.withShow(Boolean.FALSE);
          conditional.withWhen("when");
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"checkbox\",\"conditional\":{\"eq\":\"eq\",\"json\":\"json\",\"show\":false,\"when\":\"when\"}}", baos.toString());    
  }
  
  
  
}
