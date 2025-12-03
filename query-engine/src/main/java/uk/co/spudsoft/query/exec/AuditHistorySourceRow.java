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
public class AuditHistorySourceRow {

  private final String pipe;
  private final LocalDateTime timestamp;
  private final String sourceHash;
  private final String endpoint;
  private final String url;
  private final String username;
  private final String query;
  private final JsonArray arguments;

  public AuditHistorySourceRow(String pipe, LocalDateTime timestamp, String sourceHash, String endpoint, String url, String username, String query, JsonArray arguments) {
    this.pipe = pipe;
    this.timestamp = timestamp;
    this.sourceHash = sourceHash;
    this.endpoint = endpoint;
    this.url = url;
    this.username = username;
    this.query = query;
    this.arguments = arguments;
  }

  public String getPipe() {
    return pipe;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getSourceHash() {
    return sourceHash;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getQuery() {
    return query;
  }

  public JsonArray getArguments() {
    return arguments;
  }
  
}
