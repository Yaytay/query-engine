/*
 * Copyright (C) 2023 jtalbut
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.main.Version;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 * JAX-RS class implementing the REST API for reporting the user's profile data.
 * <p>
 * The information presented comes from the {@link uk.co.spudsoft.query.exec.conditions.RequestContext}.
 *
 * @author jtalbut
 */
@Path("/session")
@Timed
public class SessionHandler {

  private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
  
  private final boolean outputAllErrorMessages;
  private final boolean requireSession;

  /**
   * Constructor.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   * @param requireSession If true any requests that do not have a login session will fail.
   */
  public SessionHandler(boolean outputAllErrorMessages, boolean requireSession) {
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
  /**
   * Get the {@link Profile} data from the {@link uk.co.spudsoft.query.exec.conditions.RequestContext}.
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   */
  @GET
  @Path("/profile")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return details of the current user")
  @ApiResponse(
          responseCode = "200"
          , description = "Details of the current user."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(implementation = Profile.class)
          )
  )
  public void getProfile(
          @Suspended final AsyncResponse response
  ) {
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(Vertx.currentContext(), requireSession);
      Jwt jwt = requestContext.getJwt();

      Profile profile = new Profile(
              requestContext.getUsername()
              , requestContext.getName()
              , Version.MAVEN_PROJECT_NAME + " " + Version.MAVEN_PROJECT_VERSION
              , jwt == null ? null : jwt.getPayloadAsString()
      );

      response.resume(profile);
    } catch (Throwable ex) {
      reportError(logger, "Failed to get profile: ", response, ex, outputAllErrorMessages);
    }    
  }
  
}
