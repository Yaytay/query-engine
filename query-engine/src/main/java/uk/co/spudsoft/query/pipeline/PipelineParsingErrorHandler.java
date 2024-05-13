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
package uk.co.spudsoft.query.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link com.fasterxml.jackson.databind.deser.DeserializationProblemHandler} that just reports any errors by WARN level log messages.
 * @author jtalbut
 */
public class PipelineParsingErrorHandler extends DeserializationProblemHandler {

  private static final Logger logger = LoggerFactory.getLogger(PipelineParsingErrorHandler.class);

  @Override
  public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType, JsonToken t, JsonParser p, String failureMsg) throws IOException {
    logger.warn("Unexpected token: {} at {}: {}", targetType, buildLocation(ctxt.getParser()), failureMsg);
    return null;
  }

  @Override
  public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
    logger.warn("Unknown property: {} at {} with value {}", propertyName, buildLocation(ctxt.getParser()), p.readValueAsTree());
    return true; 
  }

  private String buildLocation(JsonParser parser) {
    
    List<String> names = new ArrayList<>();
    for (JsonStreamContext streamContext = parser.getParsingContext(); streamContext != null; streamContext = streamContext.getParent()) {
      if (streamContext.getCurrentName() != null) {
        names.add(streamContext.getCurrentName());
      }
    }
    Collections.reverse(names);
    return String.join(".", names);
  }
}
