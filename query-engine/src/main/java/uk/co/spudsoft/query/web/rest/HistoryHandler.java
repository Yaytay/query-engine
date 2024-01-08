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

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.AuditHistory;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 *
 * @author njt
 */
@Path("/history")
@Timed
public class HistoryHandler {

  private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
  
  private final Auditor auditor;
  private final boolean outputAllErrorMessages;

  public HistoryHandler(Auditor auditor, boolean outputAllErrorMessages) {
    this.auditor = auditor;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
  
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return details of the current user")
  @ApiResponse(
          responseCode = "200"
          , description = "Details of the current user."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(implementation = AuditHistory.class)
          )
  )
  public void getHistory(
          @Suspended final AsyncResponse response
          , @Context HttpServerRequest request
          , @QueryParam("maxRows") Integer maxRows
          , @QueryParam("skipRows") Integer skipRows
  ) {
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(Vertx.currentContext(), true);
      
      maxRows = boundInt(maxRows, 1000000, 0, 1000000);
      skipRows = boundInt(skipRows, 0, 0, 1000000);

      auditor.getHistory(requestContext.getIssuer(), requestContext.getSubject(), maxRows, skipRows)
              .onSuccess(history -> {
                response.resume(Response.ok(history, MediaType.APPLICATION_JSON).build());
              })
              .onFailure(ex -> {
                reportError(logger, "Failed to get history data: ", response, ex, outputAllErrorMessages);
              });
      
    } catch (Throwable ex) {
      reportError(logger, "Failed to get history data: ", response, ex, outputAllErrorMessages);
    }    
  }
  
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
