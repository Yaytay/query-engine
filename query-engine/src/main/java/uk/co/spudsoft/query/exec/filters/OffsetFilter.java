/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.filters;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorOffset;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 *
 * @author jtalbut
 */
public class OffsetFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(OffsetFilter.class);
  
  @Override
  public String getKey() {
    return "_offset";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String argument) throws IllegalArgumentException {
    int value;
    try {
      value = Integer.parseInt(argument);
    } catch (Throwable ex) {
      logger.warn("Failed to convert argument to _offset filter (\"{}\") to integer: ", argument, ex);
      throw new IllegalArgumentException("Invalid argument to _offset filter, should be an integer");
    }
    ProcessorOffset definition = ProcessorOffset.builder().offset(value).build();
    return definition.createInstance(vertx, sourceNameTracker, context);
  }
  
}
