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
import io.vertx.core.http.HttpServerResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import liquibase.exception.LiquibaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 *
 * @author jtalbut
 */
public class NullAuditor implements Auditor {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(NullAuditor.class);

  public NullAuditor() {
  }
  
  /**
   * 
   * @throws IOException if the resource accessor fails.
   * @throws SQLException if the exception thrown by the SQL driver is a non-existent driver exception.
   * @throws LiquibaseException if Liquibase is unable to prepare the database (for a non-driver error).
   * 
   */
  @Override
  public void prepare() throws IOException, SQLException, LiquibaseException {    
    logger.debug("Preparing auditor");
  }
  
  @Override
  public void recordException(RequestContext context, Throwable ex) {
    logger.info("Exception: {} {}", ex.getClass().getCanonicalName(), ex.getMessage());
  }
  
  @Override
  public Future<Void> recordRequest(RequestContext context) {
    logger.info("Request: {} {} {} {} {} {} {} {}"
            , context.getUrl()
            , context.getClientIp()
            , context.getArguments()
            , context.getHeaders()
            , context.getIssuer()
            , context.getSubject()
            , context.getNameFromJwt()
            , context.getGroups()
    );
    return Future.succeededFuture();
  }
  
  @Override
  public void recordFileDetails(RequestContext context, DirCacheTree.File file) {
    logger.info("File: {} {} {}", file.getPath(), file.getSize(), file.getModified());
  }
  
  
  @Override
  public void recordResponse(RequestContext context, HttpServerResponse response) {
    logger.info("Request complete: {} {} bytes {}", response.getStatusCode(), response.bytesWritten(), response.headers());
  }

  @Override
  public Future<Pipeline> runRateLimitRules(RequestContext context, Pipeline pipeline) {
    return Future.succeededFuture(pipeline);
  }
  
  @Override
  public Future<AuditHistory> getHistory(String issuerArg, String subjectArg, int firstRow, int maxRows) {
    return Future.succeededFuture(new AuditHistory(firstRow, 0, Collections.emptyList()));
  }
  
}
