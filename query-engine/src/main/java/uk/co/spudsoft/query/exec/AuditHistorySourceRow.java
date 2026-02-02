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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Details of a source referenced by a query.
 * 
 * @author jtalbut
 */
@Schema(
        description = """
                      Details of a source referenced by a query.
                      """
)
@SuppressFBWarnings(value={"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "This is a POJO class for providing REST output")
public class AuditHistorySourceRow {

  private final String pipe;
  private final LocalDateTime timestamp;
  private final String sourceHash;
  private final String endpoint;
  private final String url;
  private final String username;
  private final String query;
  private final List<Object> arguments;

  /**
   * Constructor.
   * 
   * @param pipe The name of the SourcePipeline being executed to reference this source.
   * @param timestamp The time of the source being referenced.
   * @param sourceHash Hash of the endpoint parameters used to deduplicate sources.
   * @param endpoint The name of the endpoint being accessed.
   * @param url The URL of the endpoint.
   * @param username The username used to connect to the endpoint.
   * @param query The query being executed against the endpoint.
   * @param arguments All arguments being passed to the query.
   */
  public AuditHistorySourceRow(String pipe, LocalDateTime timestamp, String sourceHash, String endpoint, String url, String username, String query, List<Object> arguments) {
    this.pipe = pipe;
    this.timestamp = timestamp;
    this.sourceHash = sourceHash;
    this.endpoint = endpoint;
    this.url = url;
    this.username = username;
    this.query = query;
    this.arguments = arguments;
  }

  /**
   * Get the name of the SourcePipeline being executed to reference this source.
   * @return the name of the SourcePipeline being executed to reference this source.
   */
  @Schema(
          description = """
                        The name of the SourcePipeline being executed to reference this source.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 100
  )
  public String getPipe() {
    return pipe;
  }

  /**
   * Get the time of the source being referenced.
   * @return the time of the source being referenced.
   */
  @Schema(
          description = """
                        The time of the source being referenced.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Get hash of the endpoint parameters used to dedupe sources.
   * @return hash of the endpoint parameters used to dedupe sources.
   */
  @Schema(
          description = """
                        The name of the SourcePipeline being executed to reference this source.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 64
  )
  public String getSourceHash() {
    return sourceHash;
  }

  /**
   * Get the name of the endpoint being accessed.
   * @return the name of the endpoint being accessed.
   */
  @Schema(
          description = """
                        The name of the endpoint being accessed.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 250
  )
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * Get the URL of the endpoint.
   * @return the URL of the endpoint.
   */
  @Schema(
          description = """
                        The URL of the endpoint.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 1000
  )
  public String getUrl() {
    return url;
  }

  /**
   * Get the username used to connect to the endpoint.
   * @return the username used to connect to the endpoint.
   */
  @Schema(
          description = """
                        The username used to connect to the endpoint.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
          , maxLength = 250
  )
  public String getUsername() {
    return username;
  }

  /**
   * Get the query being executed against the endpoint.
   * @return the query being executed against the endpoint.
   */
  @Schema(
          description = """
                        The query being executed against the endpoint.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
           , maxLength = 1000000
 )
  public String getQuery() {
    return query;
  }

  /**
   * Get all arguments being passed to the query.
   * @return all arguments being passed to the query.
   */
  @Schema(
          description = """
                        All arguments being passed to the query.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  @ArraySchema(
          schema = @Schema(
                  types = {"string", "number", "boolean"}
          )
  )
  public List<Object> getArguments() {
    return arguments;
  }
  
}
