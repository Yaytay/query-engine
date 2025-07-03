/*
 * Copyright (C) 2022 jtalbut
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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.healthchecks.Status;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import liquibase.exception.LiquibaseException;
import uk.co.spudsoft.dircache.DirCacheTree.File;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 * The main class for tracking requests to the Query Engine and for accessing that tracking.
 * 
 * @author jtalbut
 */
public interface Auditor {
  
  /**
   * Prepare the Auditor.
   * <P>
   * This method uses blocking methods, call before handing control to Vertx.
   * </P>
   * @throws IOException if the resource accessor fails.
   * @throws SQLException if the exception thrown by the SQL driver is a non-existent driver exception.
   * @throws LiquibaseException if Liquibase is unable to prepare the database (for a non-driver error).
   *
   */
  void prepare() throws Exception;

  /**
   * Perform a simple health check that validates that the Auditor has been prepared.
   * @param promise A Promise that will be completed with either {@link Status#KO()} or {@link Status#OK()}.
   */
  void healthCheck(Promise<Status> promise);
  
  /**
   * Wait up to timeoutMs milliseconds until all requests have a responseTime.
   * 
   * 
   * 
   * @param timeoutMs The maximum time to wait, in milliseconds.
   * @return A Future that will complete successfully when no requests have a null responseTime, or unsuccessfully after timeoutMs milliseconds.
   */
  Future<Void> waitForOutstandingRequests(long timeoutMs);
  
  /**
   * Record a request.
   * <p>
   * This must be the first method to call as it is the one that creates the row with the given {@link RequestContext#requestId}.
   * @param context The context for the request to be recorded.
   * @return a Future that will be completed when the request has been recorded.
   */
  Future<Void> recordRequest(RequestContext context);

  /**
   * Record an exception in the data store against the current {@link RequestContext#requestId}.
   * @param context The {@link RequestContext} from which to derive the {@link RequestContext#requestId}.
   * @param ex The exception to report.
   */
  void recordException(RequestContext context, Throwable ex);

  /**
   * Record details of the file found against a previously recorded {@link RequestContext#requestId}.
   * @param context The {@link RequestContext} from which to derive the {@link RequestContext#requestId}.
   * @param file The physical file containing the pipeline definition.
   * @param pipeline The pipeline definition.
   * @return a Future that will be completed when the file has been recorded.
   */
  Future<Void> recordFileDetails(RequestContext context, File file, Pipeline pipeline);

  /**
   * A record containing details of the cached output from a previous request.
   * @param auditId The {@link RequestContext#requestId} for the request the created the cache file.
   * @param cacheFile The file containing the cached output.
   * @param expiry The expiry date/time for the cached file.
   */
  record CacheDetails(String auditId, String cacheFile, LocalDateTime expiry){};
  
  /**
   * Get the most recent cache file (if any) matching the current request.
   * @param context Details of the current request to be matched against a previous run.
   * @param pipeline The pipeline being sought.
   * @return A Future that will be completed with either a {@link CacheDetails} instance or null.
   */
  Future<CacheDetails> getCacheFile(RequestContext context, Pipeline pipeline);

  /**
   * Record a cached file against the current {@link RequestContext#requestId}.
   * @param context The {@link RequestContext} from which to derive the {@link RequestContext#requestId}.
   * @param fileName The name of the file containing the cached output.
   * @param expiry The expiry date/time of the output file.
   * @return A Future that will be completed when the file has been recorded in the data store.
   */
  Future<Void> recordCacheFile(RequestContext context, String fileName, LocalDateTime expiry);
  
  /**
   * Record that the current {@link RequestContext#requestId} used the specified cache file.
   * @param context The {@link RequestContext} from which to derive the {@link RequestContext#requestId}.
   * @param fileName The cache file (created on a previous run) returned by this request.
   * @return A Future that will be completed when the file has been recorded in the data store.
   */
  Future<Void> recordCacheFileUsed(RequestContext context, String fileName);
  
  /**
   * Delete (or mark unusable) a cache file from a previous run.
   * @param auditId The {@link CacheDetails#auditId} for the request the created the cache file.
   * @return A Future that will be completed when the file has been deleted (or marked unusable) in the data store.
   */
  Future<Void> deleteCacheFile(String auditId);

  /**
   * Calculated whether or not the current request is permitted according to the rate limit rules specified in the definition.
   * @param context The {@link RequestContext} to match against previous runs.
   * @param pipeline The pipeline definition containing {@link Pipeline#rateLimitRules}.
   * @return A Future that will be completed with the passed in pipeline if the rules permit this request, or failed with a {@link uk.co.spudsoft.query.web.ServiceException} if they do not.
   */
  Future<Pipeline> runRateLimitRules(RequestContext context, Pipeline pipeline);

  /**
   * Record details of the HTTP response against the current {@link RequestContext#requestId}.
   * @param context The {@link RequestContext} from which to derive the {@link RequestContext#requestId}.
   * @param response The response details to be recorded.
   */
  void recordResponse(RequestContext context, HttpServerResponse response);
  
  /**
   * Get the details of any recorded requests matching the passed in values.
   * @param issuer The issuer to match.
   * @param subject The subject to match.
   * @param skipRows The number of rows to skip (for paging).
   * @param maxRows The maximum number of rows to return.
   * @param sortOrder The field to sort by.
   * @param sortDescending If true the sort will be in descending order.
   * @return A Future that will be completed with the audit history.
   */
  Future<AuditHistory> getHistory(String issuer, String subject, int skipRows, int maxRows, AuditHistorySortOrder sortOrder, boolean sortDescending);
 
  /**
   * Return a username with any '@&lt;domain&gt;' stripped off.
   * @param username The username, which may be an email address.
   * @return The username with anything following the first '@' removed.
   */
  static String localizeUsername(String username) {
    if (username == null) {
      return username;
    }
    String parts[] = username.split("@");
    return parts[0];
  }

  /**
   * Convert a possibly nullable List into a JsonArray.
   * @param items A List of strings that may be null.
   * @return Either null, or a JsonArray created from the List.
   */
  static JsonArray listToJson(List<String> items) {
    if (items == null) {
      return null;
    }
    return new JsonArray(items);
  }
      
}
