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

import io.vertx.core.json.JsonArray;
import java.time.LocalDateTime;

/**
 *
 * @author jtalbut
 */
public class AuditHistoryLogRow {

  private final LocalDateTime timestamp;
  private final String pipe;
  private final String level;
  private final String loggerName;
  private final String threadName;
  private final String message;
  private final JsonArray kvp;

  public AuditHistoryLogRow(LocalDateTime timestamp, String pipe, String level, String loggerName, String threadName, String message, JsonArray kvp) {
    this.timestamp = timestamp;
    this.pipe = pipe;
    this.level = level;
    this.loggerName = loggerName;
    this.threadName = threadName;
    this.message = message;
    this.kvp = kvp;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getPipe() {
    return pipe;
  }

  public String getLevel() {
    return level;
  }

  public String getLoggerName() {
    return loggerName;
  }

  public String getThreadName() {
    return threadName;
  }

  public String getMessage() {
    return message;
  }

  public JsonArray getKvp() {
    return kvp;
  }
  
  
  
}
