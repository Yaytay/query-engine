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
package uk.co.spudsoft.query.defn;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The scope of concurrency and rate limit checks.
 * 
 * A concurrency block of type 'host' will prevent that specific pipeline from being run a second time whilst a first run
 * is still in progress.
 * Similarly, a rate limit of type 'username' will prevent a pipeline being run by a given user if that user has run pipelines
 * that exceed the defined rate limit for the pipeline.
 * 
 * It is possible to combine multiple scope types in one rule.
 * 
 * @author jtalbut
 */
@Schema(description = """
                      <P>The scope of concurrency and rate limit checks.</P>
                      <P>A concurrency block of type 'host' will prevent that specific pipeline from being run a second time whilst a first run is still in progress.
                         Similarly, a rate limit of type 'username' will prevent a pipeline being run by a given user if that user has run pipelines
                         that exceed the defined rate limit for the pipeline.
                      </P>
                      <P>
                       * It is possible to combine multiple scope types in one rule.
                      </P>
                      """)
public enum RateLimitScopeType {
  
  /**
   * Runs are constrained by the host header in requests.
   */
  @Schema(description = "Runs are constrained by the host header in requests.")
  host
  , 
  /**
   * Runs are constrained by the path to the file.
   */
  @Schema(description = "Runs are constrained by the path to the file.")
  path
  , 
  /**
   * Runs are constrained by the IP address of the client.
   */
  @Schema(description = "Runs are constrained by the IP address of the client")
  clientip
  , 
  /**
   * Runs are constrained by the issuer extracted from the authorisation token.
   * This can be used on its own (it can make a good "client-wide" scope, or combined with either subject or username to identify an individual.
   */
  @Schema(description = "Runs are constrained by the issuer extracted from the authorisation token.")
  issuer
  , 
  /**
   * Runs are constrained by the subject extracted from the authorisation token.
   * This should always be used in conjunction with the issuer scope type, subjects are only unique within an issuer.
   */
  @Schema(description = "Runs are constrained by the subject extracted from the authorisation token.")
  subject
  , 
  /**
   * Runs are constrained by the username extracted from the authorisation token.
   * This should always be used in conjunction with the issuer scope type, usernames are only unique within an issuer.
   */
  @Schema(description = "Runs are constrained by the username extracted from the authorisation token.")
  username
  
}
