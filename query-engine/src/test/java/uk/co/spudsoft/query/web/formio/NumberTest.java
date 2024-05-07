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
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


/**
 *
 * @author jtalbut
 */
public class NumberTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testAddNumberValidate_WithCustom()  throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withCustom("b");
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"custom\":\"b\"}}", baos.toString());    
  }
  
  @Test
  public void testAddNumberValidate_WithMin()  throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMin(1);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"min\":1}}", baos.toString());    
  }
  
  @Test
  public void testAddNumberValidate_WithMax()  throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMax(1L);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"max\":1}}", baos.toString());    
    
    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMax(new BigDecimal("3.141"));
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"max\":3.141}}", baos.toString());        
    
    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMax(new BigInteger("3141"));
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"max\":3141}}", baos.toString());        
    
    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMax(Float.valueOf("3.141"));
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"max\":3.141}}", baos.toString());        
    
    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withMax((short) 3141);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"max\":3141}}", baos.toString());        
  }
  
  @Test
  public void testAddNumberValidate_WithStep()  throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (Number field = new Number(generator)) {
        try(Number.NumberValidation v = field.addNumberValidate()) {
          v.withStep(1.2);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"number\",\"validate\":{\"step\":1.2}}", baos.toString());    
  }
}
