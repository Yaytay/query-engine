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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A single log record from a single request.
 * 
 * @author jtalbut
 */
@Schema(
        description = """
                      A single log record from a single request.
                      """
)
@SuppressFBWarnings(value={"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "This is a POJO class for providing REST output")
public class AuditHistoryLogRow {

  private final LocalDateTime timestamp;
  private final String pipe;
  private final String level;
  private final String loggerName;
  private final String threadName;
  private final String message;
  private final List<AuditLogMessage.PrimitiveKeyValuePair> kvp;

  /**
   * Constructor.
   * 
   * @param timestamp The time of the log event.
   * @param pipe The pipe being run from the pipeline.
   * @param level The log level of this log record.
   * @param loggerName The name of the logger (typically the class).
   * @param threadName Name of the thread recording the message.
   * @param message The log message.
   * @param kvp Any KeyValuePair data associated with the log message.
   */
  public AuditHistoryLogRow(LocalDateTime timestamp, String pipe, String level, String loggerName, String threadName, String message, List<AuditLogMessage.PrimitiveKeyValuePair> kvp) {
    this.timestamp = timestamp;
    this.pipe = pipe;
    this.level = level;
    this.loggerName = loggerName;
    this.threadName = threadName;
    this.message = message;
    this.kvp = kvp;
  }

  /**
   * Get the time of the log event.
   * @return The time of the log event.
   */
  @Schema(
          description = """
                        The time of the log event.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Get the pipe being run from the pipeline.
   * @return the pipe being run from the pipeline.
   */
  @Schema(
          description = """
                        The pipe being run from the pipeline.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 100
  )
  public String getPipe() {
    return pipe;
  }

  /**
   * Get the log level of this log record.
   * @return the log level of this log record.
   */
  @Schema(
          description = """
                        The log level of this log record.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 16
  )
  public String getLevel() {
    return level;
  }

  /**
   * Get the name of the logger (typically the class).
   * @return the name of the logger (typically the class).
   */
  @Schema(
          description = """
                        The name of the logger (typically the class).
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 250
  )
  public String getLoggerName() {
    return loggerName;
  }

  /**
   * Get the name of the thread recording the message.
   * @return the name of the thread recording the message.
   */
  @Schema(
          description = """
                        The name of the thread recording the message.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 250
  )
  public String getThreadName() {
    return threadName;
  }

  /**
   * Get the log message.
   * @return the log message.
   */
  @Schema(
          description = """
                        The log message.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 1000000
  )
  public String getMessage() {
    return message;
  }

  /**
   * Get any KeyValuePair data associated with the log message.
   * @return any KeyValuePair data associated with the log message.
   */
  @Schema(
          description = """
                        Any KeyValuePair data associated with the log message.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public List<AuditLogMessage.PrimitiveKeyValuePair> getKvp() {
    return kvp;
  }
  
}
