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
import liquibase.exception.LiquibaseException;
import uk.co.spudsoft.dircache.DirCacheTree.File;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 *
 * @author jtalbut
 */
public interface Auditor {

  /**
   *
   * @throws IOException if the resource accessor fails.
   * @throws SQLException if the exception thrown by the SQL driver is a non-existent driver exception.
   * @throws LiquibaseException if Liquibase is unable to prepare the database (for a non-driver error).
   *
   */
  void prepare() throws IOException, SQLException, LiquibaseException;

  void recordException(RequestContext context, Throwable ex);

  void recordFileDetails(RequestContext context, File file);

  Future<Void> recordRequest(RequestContext context);

  void recordResponse(RequestContext context, HttpServerResponse response);
  
}
