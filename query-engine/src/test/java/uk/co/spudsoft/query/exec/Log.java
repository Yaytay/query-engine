/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Wrapper for an slf4j Logger that add KVP values specific to Query Engine.
 * @author jtalbut
 */
public class Log {
  
  private final Logger logger;
  
  /**
   * 
   * @param clazz
   * @return 
   */
  public static Log create(Class<?> clazz) {
    return new Log(clazz);
  }

  private Log(Class<?> clazz) {
    this.logger = LoggerFactory.getLogger(clazz);
  }
  
  @CheckReturnValue
  public LoggingEventBuilder trace(RequestContext requestContext, String sourcePipeline) {
    return level(Level.TRACE, requestContext, sourcePipeline);
  }
    
  @CheckReturnValue
  public LoggingEventBuilder debug(RequestContext requestContext, String sourcePipeline) {
    return level(Level.DEBUG, requestContext, sourcePipeline);
  }
    
  @CheckReturnValue
  public LoggingEventBuilder info(RequestContext requestContext, String sourcePipeline) {
    return level(Level.INFO, requestContext, sourcePipeline);
  }
    
  @CheckReturnValue
  public LoggingEventBuilder warn(RequestContext requestContext, String sourcePipeline) {
    return level(Level.WARN, requestContext, sourcePipeline);
  }
    
  @CheckReturnValue
  public LoggingEventBuilder error(RequestContext requestContext, String sourcePipeline) {
    return level(Level.ERROR, requestContext, sourcePipeline);
  }
    
  @CheckReturnValue
  public LoggingEventBuilder level(Level level, RequestContext requestContext, String sourcePipeline) {
    LoggingEventBuilder builder = logger.atDebug();
    if (requestContext != null) {
      String requestid = requestContext.getRequestId();
      if (requestid != null) {
        builder = builder.addKeyValue("requestId", requestid);
      }
      String runid = requestContext.getRunID();
      if (runid != null) {
        builder = builder.addKeyValue("runId", runid);
      }
    }
    if (sourcePipeline != null) {
      builder = builder.addKeyValue("pipe", sourcePipeline);
    }
    return builder;
  }
  
}
