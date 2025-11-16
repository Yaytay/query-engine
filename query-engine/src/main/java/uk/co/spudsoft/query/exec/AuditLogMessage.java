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

import java.time.LocalDateTime;
import uk.co.spudsoft.query.defn.SourcePipeline;

/**
 * An audit record to be stored for later viewing.
 * 
 * @author jtalbut
 */
public class AuditLogMessage {
  
  private final String pipe;
  private final LocalDateTime timestamp;
  private final String level;
  private final String loggerName;
  private final String threadName;
  private final String kvpDataJson;
  private final String message;

  /**
   * Constructor.
   * 
   * @param pipe The name of the {@link SourcePipeline} that generated the message.
   * @param timestamp The timestamp for when the event was raised.
   * @param level The level at which the event was raised.
   * @param message The formatted message from the event.
   * @param loggerName The logger name captured by slf4j.
   * @param threadName The thread name captured by slf4j.
   * @param kvpDataJson The KeyValuePair data captured by slf4j, as JSON.
   */
  public AuditLogMessage(String pipe, LocalDateTime timestamp, String level, String loggerName, String threadName, String kvpDataJson, String message) {
    this.pipe = pipe;
    this.timestamp = timestamp;
    this.level = level;
    this.loggerName = loggerName;
    this.threadName = threadName;
    this.kvpDataJson = kvpDataJson;
    this.message = message;
  }

  /**
   * Get the name of the {@link SourcePipeline} that generated the message.
   * @return the name of the {@link SourcePipeline} that generated the message.
   */
  public String getPipe() {
    return pipe;
  }

  /**
   * Get the timestamp for when the event was raised.
   * @return the timestamp for when the event was raised.
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Get the level at which the event was raised.
   * @return the level at which the event was raised.
   */
  public String getLevel() {
    return level;
  }  

  /**
   * Get the logger name captured by slf4j.
   * @return the logger name captured by slf4j.
   */
  public String getLoggerName() {
    return loggerName;
  }

  /**
   * Get the KeyValuePair data captured by slf4j as json.
   * @return the KeyValuePair data captured by slf4j as json.
   */
  public String getKvpDataJson() {
    return kvpDataJson;
  }

  /**
   * Get the thread name captured by slf4j.
   * @return the thread name captured by slf4j.
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * Get the formatted message from the event.
   * @return the formatted message from the event.
   */
  public String getMessage() {
    return message;
  }
  
}
