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
package uk.co.spudsoft.query.web.rest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.AuditHistory;
import uk.co.spudsoft.query.exec.AuditHistorySortOrder;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.context.RequestContext;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 * JAX-RS class implementing the REST API for outputting the history data.
 * <p>
 * The history data is entirely dependent upon the {@link uk.co.spudsoft.query.exec.Auditor} and the data it records.
 *
 * @author jtalbut
 */
@Path("/history")
@Timed
public class HistoryHandler {

  private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
  
  private final Auditor auditor;
  private final boolean outputAllErrorMessages;

  /**
   * Constructor.
   * @param auditor Auditor interface for tracking requests.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Only method called on auditor is getHistory")
  public HistoryHandler(Auditor auditor, boolean outputAllErrorMessages) {
    this.auditor = auditor;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
  
  /**
   * Return history rows matching the criteria.
   * <p>
   * This probably uses dynamic SQL for the sorting, which is secure because of the use of the enum for sortOrder.
   * 
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param skipRows Number of rows to skip, for paging.
   * @param maxRows Maximum number of rows to return, for paging.
   * @param sortOrder The field to sort by.
   * @param sortDescending If true the results are sorted in descending order.
   * @param filterTimestampStart Only provide results after this timestamp.
   * @param filterTimestampEnd Only provide results before this timestamp.
   * @param filterIssuer Only provide results with this issuer.
   * @param filterName Only provide results with this username.
   * @param filterPath Only provide results with a path that begins with this.
   * @param filterResponseCode Only provide results with this response code.
   */
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Return history rows matching the criteria."
          , description = """
                          Will only ever return data for the user making the request (token subject must match).
                          <p>
                          By default returns the first 1000000 rows sorted by timestsamp with the most recent data first.
                          """
  )
  @ApiResponse(
          responseCode = "200"
          , description = "Details of the current user."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(implementation = AuditHistory.class)
          )
  )
  public void getHistory(
          @Context RoutingContext routingContext
          , @Suspended final AsyncResponse response
          , @Schema(
                  description = "The number of rows to skip in the results, to implement paging"
                  , minimum = "0"
                  , maximum = "1000000"
                  , defaultValue = "0"
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("skipRows") 
            Integer skipRows
          , @Schema(
                  description = "The maximum number of rows to return, to implement paging"
                  , minimum = "0"
                  , maximum = "1000000"
                  , defaultValue = "1000000"
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("maxRows") 
            Integer maxRows
          , @Schema(
                  description = "Sort order for the history data."
                  , defaultValue = "timestamp"
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("sort") 
            AuditHistorySortOrder sortOrder
          , @Schema(
                  description = "Whether the sort should be in descending order or not (by default it is)."
                  , defaultValue = "true"
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("desc") 
            Boolean sortDescending
          , @Schema(
                  description = """
                                Only provide results after this timestamp.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterTimestampStart") 
            String filterTimestampStart
          , @Schema(
                  description = """
                                Only provide results before this timestamp.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterTimestampEnd") 
            String filterTimestampEnd
          , @Schema(
                  description = """
                                Only provide results with this issuer.
                                <P>
                                Note that only the global operator can see history for requests outside of their own issuer.
                                If this parameter is set to a value that does not match the issuer in the callers token no results will be returned unless the request is from a global operator.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterIssuer") 
            String filterIssuer
          , @Schema(
                  description = """
                                Only provide results with this username.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterName") 
            String filterName
          , @Schema(
                  description = """
                                Only provide results with a path that begins with this.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterPath") 
            String filterPath
          , @Schema(
                  description = """
                                Only provide results with this response code.
                                """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("filterResponseCode") 
            Integer filterResponseCode
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(routingContext, true);
      
      skipRows = boundInt(skipRows, 0, 0, 1000000);
      maxRows = boundInt(maxRows, 1000000, 0, 1000000);
      if (sortOrder == null) {
        sortOrder = AuditHistorySortOrder.timestamp;
      }
      if (sortDescending == null) {
        sortDescending = Boolean.TRUE;
      }
      
      LocalDateTime filterTimestampStartLdt = parseIso8601("filterTimestampStart", filterTimestampStart);
      LocalDateTime filterTimestampEndLdt = parseIso8601("filterTimestampEnd", filterTimestampEnd);

      Auditor.HistoryFilters filters = new Auditor.HistoryFilters(filterTimestampStartLdt, filterTimestampEndLdt, filterIssuer, filterName, filterPath, filterResponseCode);
      
      auditor.getHistory(requestContext, requestContext.getIssuer(), requestContext.getSubject(), skipRows, maxRows, sortOrder, sortDescending, filters)
              .onSuccess(history -> {
                response.resume(Response.ok(history, MediaType.APPLICATION_JSON).build());
              })
              .onFailure(ex -> {
                reportError(unauthedRequestContext, logger, "Failed to get history data: ", response, ex, outputAllErrorMessages);
              });
      
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to get history data: ", response, ex, outputAllErrorMessages);
    }    
  }
  
  /**
   * Parses an ISO8601 string which can be either a specific date/time or a duration.
   *
   * @param name The name of the parameter to report in any exception thrown.
   * @param input ISO8601 string (e.g., "2026-01-07T12:00:00Z" or "PT1H").
   * @return LocalDateTime in UTC, or null if input is null.
   */
  public static LocalDateTime parseIso8601(String name, String input) {
    if (input == null || input.isBlank()) {
      return null;
    }

    // 1. Try to parse as a Duration (starts with 'P')
    if (input.startsWith("P") || input.startsWith("-P")) {
      try {
        Duration duration = Duration.parse(input);
        return LocalDateTime.now(ZoneOffset.UTC).plus(duration);
      } catch (DateTimeParseException e) {
        // Fall through to try date/time parsing
      }
    }

    // 2. Try to parse as a Date/Time
    try {
      // OffsetDateTime handles the 'Z' or '+01:00' suffix
      return OffsetDateTime.parse(input).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    } catch (DateTimeParseException e) {
      try {
        // If it fails, try parsing as LocalDateTime (no timezone specified)
        // and treat as UTC as requested
        return LocalDateTime.parse(input);
      } catch (DateTimeParseException e2) {
        throw new IllegalArgumentException(name + " is neither a valid ISO8601 date/time nor a duration: " + input, e2);
      }
    }
  }

  /**
   * Return an integer value that is definitely not null and is &gt;= min and &lt;= max.
   * @param input The input value, which may be null.
   * @param defaultValue The value to return if input is null.
   * @param min The minimum permitted value for input.
   * @param max The maximum permitted value for input.
   * @return Either input, defaultValue, min or max.
   */
  static int boundInt(Integer input, int defaultValue, int min, int max) {
    if (input == null) {
      return defaultValue;
    }
    if (input < min) {
      return min;
    }
    if (input > max) {
      return max;
    }
    return input;
  }
  
}
