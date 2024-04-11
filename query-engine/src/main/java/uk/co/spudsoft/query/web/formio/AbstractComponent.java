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

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Base class for outputting a formio component.
 * 
 * @author jtalbut
 * @param <T> The class that derives from this class in order for the with methods to return the appropriate type.
 */
@SuppressWarnings("unchecked")
public class AbstractComponent<T extends AbstractComponent<T>> implements Closeable {
  
  protected final JsonGenerator generator;
  
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  protected AbstractComponent(JsonGenerator generator) throws IOException {
    this.generator = generator;
    generator.writeStartObject();
  }

  @Override
  public void close() throws IOException {
    generator.writeEndObject();
  }
  
  protected T with(String key, Integer value) throws IOException {
    if (value != null) {
      generator.writeNumberField(key, value);
    }
    return (T) this;    
  }

  protected T with(String key, java.lang.Number value) throws IOException {
    if (value != null) {
      if (value instanceof Integer i) {
        generator.writeNumberField(key, i);        
      } else if (value instanceof Double i) {
        generator.writeNumberField(key, i);        
      } else if (value instanceof Long i) {
        generator.writeNumberField(key, i);        
      } else if (value instanceof BigDecimal i) {
        generator.writeNumberField(key, i);
      } else if (value instanceof BigInteger i) {
        generator.writeNumberField(key, i);        
      } else if (value instanceof Float i) {
        generator.writeNumberField(key, i);        
      } else if (value instanceof Short i) {
        generator.writeNumberField(key, i);        
      }
    }
    return (T) this;    
  }

  protected T with(String key, Boolean value) throws IOException {
    if (value != null) {
      generator.writeBooleanField(key, value);
    }
    return (T) this;    
  }
  
  protected T with(String key, String value) throws IOException {
    if (value != null) {
      generator.writeStringField(key, value);
    }
    return (T) this;    
  }
  
  protected T with(String key, LocalDateTime value) throws IOException {
    if (value != null) {
      generator.writeStringField(key, value.toString());
    }
    return (T) this;    
  }
  
}
