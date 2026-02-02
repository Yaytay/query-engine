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

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.event.KeyValuePair;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

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
  private final ImmutableList<PrimitiveKeyValuePair> kvpData;
  private final String message;

  /**
   * A KeyValuePair class that can only contain values that are instances of String, Number or Boolean (or null).
   * 
   * 
   */
  @Schema (
          description = """
                         A Key/Value pair recorded at the time of the log message.
                         """
  )
  public static class PrimitiveKeyValuePair {
    private final String key;
    private final Object value;

    /**
     * Constructor.
     * 
     * If the value is not null and is not and instance of String, Number or Boolean (or null) then the value stored will be value.toString().
     * 
     * @param key The Key for this Key/Value pair recorded at the time of the log message.
     * @param value The Value for this Key/Value pair recorded at the time of the log message.
     */
    public PrimitiveKeyValuePair(String key, Object value) {
      if (key.length() > 1000) {
        key = key.substring(0, 999);
      }
      this.key = key;
      if (
              value == null
              || value instanceof String
              || value instanceof Number
              || value instanceof Boolean
          ) {
        this.value = value;
      } else {
        this.value = value.toString();
      }
    }

    /**
     * Get the Key for this Key/Value pair recorded at the time of the log message.
     * @return the Key for this Key/Value pair recorded at the time of the log message.
     */
    @Schema (
            description = """
                           The Key for this Key/Value pair recorded at the time of the log message.
                           """
            , maxLength = 1000
    )
    public String getKey() {
      return key;
    }

    /**
     * Get the Value for this Key/Value pair recorded at the time of the log message.
     * If not null, this will be an instance of either String, Number or Boolean.
     * @return the Value for this Key/Value pair recorded at the time of the log message.
     */
    @Schema (
            description = """
                           The Value for this Key/Value pair recorded at the time of the log message.
                           """
            , types = {"string", "number", "boolean"}
    )
    public Object getValue() {
      return value;
    }        
  }
  
  /**
   * Constructor.
   * 
   * @param pipe The name of the {@link SourcePipeline} that generated the message.
   * @param timestamp The timestamp for when the event was raised.
   * @param level The level at which the event was raised.
   * @param message The formatted message from the event.
   * @param loggerName The logger name captured by slf4j.
   * @param threadName The thread name captured by slf4j.
   * @param kvpData The KeyValuePair data captured by slf4j.
   */
  public AuditLogMessage(String pipe, LocalDateTime timestamp, String level, String loggerName, String threadName, Collection<KeyValuePair> kvpData, String message) {
    this.pipe = pipe;
    this.timestamp = timestamp;
    this.level = level;
    this.loggerName = loggerName;
    this.threadName = threadName;
    if (kvpData == null) {
      this.kvpData = ImmutableList.of();
    } else {
      this.kvpData = ImmutableCollectionTools.copy(kvpData.stream().map(kvp -> new PrimitiveKeyValuePair(kvp.key, kvp.value)).collect(Collectors.toList()));
    }
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
   * Get the KeyValuePair data captured by slf4j.
   * @return the KeyValuePair data captured by slf4j.
   */
  public List<PrimitiveKeyValuePair> getKvpData() {
    return kvpData;
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
