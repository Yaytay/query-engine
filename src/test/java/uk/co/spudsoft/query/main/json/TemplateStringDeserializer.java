/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

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
        } catch(Throwable ex) {
          logger.warn("Failed to process StringTemplate {}: ", value, ex);
        }
      } else {
        return value;
      }
    }
    return super.deserialize(p, ctxt);
  }

}
