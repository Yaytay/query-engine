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
package uk.co.spudsoft.query.logging;

import org.slf4j.Logger;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Query-Engine-specific Log functionality.
 * 
 * The static {@link #decorate(org.slf4j.spi.LoggingEventBuilder, uk.co.spudsoft.query.exec.context.PipelineContext)} method encapsulates all the logic of this class.
 * The class can also be used as a wrapper around an slf4j {@link Logger} to make logging calls tidier.
 * 
 * @author jtalbut
 */
public class Log {

  private final Logger logger;
  private final PipelineContext pipelineContext;
  
  /**
   * Constructor.
   * 
   * @param logger The slf4j Logger to wrap.
   * @param pipelineContext The context in which this {@link uk.co.spudsoft.query.defn.SourcePipeline} is being run.
   */
  public Log(Logger logger, PipelineContext pipelineContext) {
    this.logger = logger;
    this.pipelineContext = pipelineContext;
  }
  
  /**
   * Create a decorated LoggingEventBuilder at trace level.
   * 
   * @return a decorated LoggingEventBuilder at trace level. 
   */
  @CheckReturnValue
  public LoggingEventBuilder trace() {
    return decorate(logger.atTrace(), pipelineContext);
  }
  
  /**
   * Create a decorated LoggingEventBuilder at debug level.
   * 
   * @return a decorated LoggingEventBuilder at debug level. 
   */
  @CheckReturnValue
  public LoggingEventBuilder debug() {
    return decorate(logger.atDebug(), pipelineContext);
  }
  
  /**
   * Create a decorated LoggingEventBuilder at info level.
   * 
   * @return a decorated LoggingEventBuilder at info level. 
   */
  @CheckReturnValue
  public LoggingEventBuilder info() {
    return decorate(logger.atInfo(), pipelineContext);
  }
  
  /**
   * Create a decorated LoggingEventBuilder at warn level.
   * 
   * @return a decorated LoggingEventBuilder at warn level. 
   */
  @CheckReturnValue
  public LoggingEventBuilder warn() {
    return decorate(logger.atWarn(), pipelineContext);
  }
  
  /**
   * Create a decorated LoggingEventBuilder at error level.
   * 
   * @return a decorated LoggingEventBuilder at error level. 
   */
  @CheckReturnValue
  public LoggingEventBuilder error() {
    return decorate(logger.atError(), pipelineContext);
  }
  
  
  /**
   * Add KVP data to a LoggingEvent.
   * @param event The LoggingEvent to be decorated.
   * @param pipelineContext The source of the KVP data.
   * @return the decorated LoggingEvent.
   */
  @CheckReturnValue
  public static LoggingEventBuilder decorate(LoggingEventBuilder event, PipelineContext pipelineContext) {
    if (event != NOPLoggingEventBuilder.singleton()) {
      if (pipelineContext != null) {
        event = decorate(event, pipelineContext.getRequestContext());
        String pipe = pipelineContext.getPipe();
        if (pipe != null) {
          event = event.addKeyValue("pipeline", pipe);
        }
      }
    }
    return event;
  }
  
  /**
   * Add KVP data to a LoggingEvent.
   * @param event The LoggingEvent to be decorated.
   * @param requestContext The source of the KVP data.
   * @return the decorated LoggingEvent.
   */
  @CheckReturnValue
  public static LoggingEventBuilder decorate(LoggingEventBuilder event, RequestContext requestContext) {
    if (requestContext != null) {
      String requestId = requestContext.getRequestId();
      if (requestId != null) {
        event = event.addKeyValue("requestId", requestId);
      }
      String runId = requestContext.getRunID();
      if (runId != null) {
        event = event.addKeyValue("runId", runId);
      }
    }
    return event;
  }
  
}
