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
package uk.co.spudsoft.query.exec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Record of a request made against the Query Engine.
 * @author njt
 */
@Schema(
        description = """
                      Record of a request made against the Query Engine.
                      """
)
public class AuditHistory {
  
  private final LocalDateTime timestamp;
  private final String id;
  private final String path;
  private final ObjectNode arguments;
  private final String host;
  private final String issuer;
  private final String subject;
  private final String username;
  private final String name;
  private final int responseCode;
  private final long responseRows;
  private final long responseSize;
  private final Duration responseStreamStart;
  private final Duration responseDuration;

  /**
   * Constructor.
   * @param timestamp Timestamp of the request.
   * @param id Unique ID for the request.
   * @param path Path to the pipeline.
   * @param arguments Arguments passed to the pipeline.
   * @param host The issuer of the token used to authenticate the user.
   * @param issuer The issuer of the token used to authenticate the user.
   * @param subject The subject from the token (unique ID for the user within the issuer).
   * @param username The username of the user making the request.
   * @param name The human name of the user making the request.
   * @param responseCode The HTTP response code that the request generated.
   * @param responseRows The number of rows returned by the request.
   * @param responseSize The number of bytes returned by the request.
   * @param responseStreamStartMillis The time (in milli seconds) between the request being made and the first row being returned.
   * @param responseDurationMillis The time (in milli seconds) between the request being made and the final row being returned.
   */
  @SuppressFBWarnings(value="EI_EXPOSE_REP2", justification = "AuditHistory is just a carrier")
  public AuditHistory(LocalDateTime timestamp, String id, String path, ObjectNode arguments, String host, String issuer, String subject, String username, String name, int responseCode, long responseRows, long responseSize, long responseStreamStartMillis, long responseDurationMillis) {
    this.timestamp = timestamp;
    this.id = id;
    this.path = path;
    this.arguments = arguments;
    this.host = host;
    this.issuer = issuer;
    this.subject = subject;
    this.username = username;
    this.name = name;
    this.responseCode = responseCode;
    this.responseRows = responseRows;
    this.responseSize = responseSize;
    this.responseStreamStart = Duration.ofMillis(responseStreamStartMillis);
    this.responseDuration = Duration.ofMillis(responseDurationMillis);
  }

  /**
   * Get the timestamp of the request.
   * @return The timestamp of the request.
   */
  @Schema(
          description = """
                        Timestamp of the request.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Get the unique ID for the request.
   * @return The unique ID for the request.
   */
  @Schema(
          description = """
                        Unique ID for the request.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getId() {
    return id;
  }

  /**
   * Get the path to the pipeline.
   * @return The path to the pipeline.
   */
  @Schema(
          description = """
                        Path to the pipeline.
                        """
          , maxLength = 260
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getPath() {
    return path;
  }

  /**
   * Get the arguments passed to the pipeline.
   * @return the arguments passed to the pipeline.
   */
  @Schema(
          description = """
                        Arguments passed to the pipeline.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  @SuppressFBWarnings(value="EI_EXPOSE_REP", justification = "AuditHistory is just a carrier")
  public ObjectNode getArguments() {
    return arguments;
  }

  /**
   * Get the host from the request.
   * @return the host from the request.
   */
  @Schema(
          description = """
                        The host from the request.
                        """
          , maxLength = 260
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getHost() {
    return host;
  }

  /**
   * Get the issuer of the token used to authenticate the user.
   * @return the issuer of the token used to authenticate the user.
   */
  @Schema(
          description = """
                        The issuer of the token used to authenticate the user.
                        """
          , maxLength = 1000
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getIssuer() {
    return issuer;
  }

  /**
   * Get the subject from the token (unique ID for the user within the issuer).
   * This is usually the same as the username, but for some issuers it differs.
   * @return the subject from the token (unique ID for the user within the issuer).
   */
  @Schema(
          description = """
                        The subject from the token (unique ID for the user within the issuer).
                        """
          , maxLength = 1000
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getSubject() {
    return subject;
  }

  /**
   * Get the user name of the user making the request.
   * @return the user name of the user making the request.
   */
  @Schema(
          description = """
                        The user name of the user making the request.
                        """
          , maxLength = 1000
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getUsername() {
    return username;
  }

  /**
   * Get the human name of the user making the request.
   * @return the human name of the user making the request.
   */
  @Schema(
          description = """
                        The human name of the user making the request.
                        """
          , maxLength = 500
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getName() {
    return name;
  }

  /**
   * Get the HTTP response code that the request generated.
   * @return the HTTP response code that the request generated.
   */
  @Schema(
          description = """
                        The HTTP response code that the request generated.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Get the number of rows returned by the request.
   * @return the number of rows returned by the request.
   */
  @Schema(
          description = """
                        The number of rows returned by the request.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public long getResponseRows() {
    return responseRows;
  }

  /**
   * Get the number of bytes returned by the request.
   * @return the number of bytes returned by the request.
   */
  @Schema(
          description = """
                        The number of bytes returned by the request.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public long getResponseSize() {
    return responseSize;
  }

  /**
   * Get the time between the request being made and the first row being returned.
   * @return the time between the request being made and the first row being returned.
   */
  @Schema(
          description = """
                        The time between the request being made and the first row being returned.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public Duration getResponseStreamStart() {
    return responseStreamStart;
  }

  /**
   * Get the time between the request being made and the final row being returned.
   * @return the time between the request being made and the final row being returned.
   */
  @Schema(
          description = """
                        The time between the request being made and the final row being returned.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public Duration getResponseDuration() {
    return responseDuration;
  }

}
