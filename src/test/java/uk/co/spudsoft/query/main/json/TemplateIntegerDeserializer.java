/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class TemplateIntegerDeserializer extends StdScalarDeserializer<Integer> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplateIntegerDeserializer.class);
  private static final long serialVersionUID = 1L;

  final static NumberDeserializers.IntegerDeserializer primitiveWrapped = new NumberDeserializers.IntegerDeserializer(Integer.TYPE, 0);
  final static NumberDeserializers.IntegerDeserializer wrapperWrapped = new NumberDeserializers.IntegerDeserializer(Integer.class, null);

  public TemplateIntegerDeserializer(Class<?> vc) {
    super(vc);
  }
  
  @Override
  public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    if (p.isExpectedNumberIntToken()) {
      int value = p.getIntValue();
      logger.debug("{}: {}", ctxt, value);
      return value;
    }
    return _parseInteger(p, ctxt, Integer.class);
  }

}
