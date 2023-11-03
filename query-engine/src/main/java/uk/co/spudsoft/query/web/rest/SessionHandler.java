/*
 * Copyright (C) 2023 njt
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
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 *
 * @author njt
 */
@Path("/session")
@Timed
public class SessionHandler {

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
          , @Context HttpServerRequest request
  ) {
    RequestContext requestContext = Vertx.currentContext().getLocal("req");
    
    Profile profile = new Profile();
    profile.setUsername(requestContext.getUsername());
    profile.setFullname(requestContext.getNameFromJwt());
    
    response.resume(profile);
    
  }
  
}
