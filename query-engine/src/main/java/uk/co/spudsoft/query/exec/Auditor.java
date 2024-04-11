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

  void healthCheck(Promise<Status> promise);
  
  void recordException(RequestContext context, Throwable ex);

  Future<Void> recordFileDetails(RequestContext context, File file, Pipeline pipeline);

  Future<Void> recordRequest(RequestContext context);

  record CacheDetails(String auditId, String cacheFile, LocalDateTime expiry){};
  
  Future<CacheDetails> getCacheFile(RequestContext context, Pipeline pipeline);

  Future<Void> recordCacheFile(RequestContext context, String fileName, LocalDateTime expiry);
  
  Future<Void> recordCacheFileUsed(RequestContext context, String fileName);
  
  Future<Void> deleteCacheFile(String auditId);

  Future<Pipeline> runRateLimitRules(RequestContext context, Pipeline pipeline);

  void recordResponse(RequestContext context, HttpServerResponse response);
  
  Future<AuditHistory> getHistory(String issuer, String subject, int skipRows, int maxRows, AuditHistorySortOrder sortOrder, boolean sortDescending);
 
  static String localizeUsername(String username) {
    if (username == null) {
      return username;
    }
    String parts[] = username.split("@");
    return parts[0];
  }

  static JsonArray listToJson(List<String> items) {
    if (items == null) {
      return null;
    }
    return new JsonArray(items);
  }
      
}
