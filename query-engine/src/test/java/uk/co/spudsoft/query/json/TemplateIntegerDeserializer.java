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
