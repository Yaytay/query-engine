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
 * <p>
 * The key construct in the use of these formio components classes is try-with-resources, which is used to guarantee the appropriate closure of each JSON object.
 * 
 * @author jtalbut
 * @param <T> The class that derives from this class in order for the with methods to return the appropriate type.
 */
@SuppressWarnings("unchecked")
public class AbstractComponent<T extends AbstractComponent<T>> implements Closeable {
  
  /**
   * The {@link com.fasterxml.jackson.core.JsonGenerator} used in the construction of the output JSON.
   */
  protected final JsonGenerator generator;
  
  /**
   * Constructor.
   * 
   * @param generator The Jackson {@link com.fasterxml.jackson.core.JsonGenerator} for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  protected AbstractComponent(JsonGenerator generator) throws IOException {
    this.generator = generator;
    generator.writeStartObject();
  }

  @Override
  public void close() throws IOException {
    generator.writeEndObject();
  }
  
  /**
   * Output an Integer value as a JSON field.
   * @param key The key for the JSON field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  protected T with(String key, Integer value) throws IOException {
    if (value != null) {
      generator.writeNumberField(key, value);
    }
    return (T) this;    
  }

  /**
   * Output a Number value as a JSON field.
   * @param key The key for the JSON field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
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

  /**
   * Output a Boolean value as a JSON field.
   * @param key The key for the JSON field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  protected T with(String key, Boolean value) throws IOException {
    if (value != null) {
      generator.writeBooleanField(key, value);
    }
    return (T) this;    
  }
  
  /**
   * Output a String value as a JSON field.
   * @param key The key for the JSON field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  protected T with(String key, String value) throws IOException {
    if (value != null) {
      generator.writeStringField(key, value);
    }
    return (T) this;    
  }
  
  /**
   * Output a LocalDateTime value as a JSON field.
   * @param key The key for the JSON field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  protected T with(String key, LocalDateTime value) throws IOException {
    if (value != null) {
      generator.writeStringField(key, value.toString());
    }
    return (T) this;    
  }
  
}
