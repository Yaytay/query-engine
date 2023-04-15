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
package uk.co.spudsoft.query.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;


/**
 *
 * @author jtalbut
 */
public class TemplateStringDeserializer extends StringDeserializer {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplateStringDeserializer.class);
  private static final long serialVersionUID = 1L;

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    if (p.hasToken(JsonToken.VALUE_STRING)) {
      String value = p.getText();
      
      if (value.startsWith("\\\\")) {
        return value.substring(1);
      } else if (value.startsWith("\\$")) {
        return value.substring(2);
      } else if (value.startsWith("$")) {
        try {
          ST template = new ST(value.substring(1));
          Context context = Vertx.currentContext();
          if (context  != null) {
            Object args = context.get("ARGUMENTS");
            if (args instanceof Map<?,?> map) {
              map.forEach((k, v) -> {
                if (k instanceof String) {
                  template.add((String) k, v);
                }
              });
            }
            return template.render();
          }
        } catch (Throwable ex) {
          logger.warn("Failed to process StringTemplate {}: ", value, ex);
        }
      } else {
        return value;
      }
    }
    return super.deserialize(p, ctxt);
  }

}
