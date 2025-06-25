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
package uk.co.spudsoft.query.main.sample;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample data load for Microsoft SQL Server.
 *
 * @author jtalbut
 */
public class SampleDataLoaderMsSQL extends AbstractSampleDataLoader {

  /**
   * Constructor.
   */
  public SampleDataLoaderMsSQL() {
  }

  @Override
  public String getName() {
    return "SQL Server";
  }

  @Override
  protected String getScript() {
    return "/sampleData/MS SQL Test Structures.sql";
  }

  @Override
  protected String getSampleTableName() {
    return "stock";
  }

  @Override
  protected String getJdbcUrlPrefix() {
    return "jdbc:sqlserver://";
  }

  @Override
  public String getIdentifierQuote() {
    return "[";
  }

  @Override
  protected String getTableExistsQuery(String tableName) {
    // SQL Server uses sys.tables instead of information_schema.tables for better performance
    return "SELECT 1 FROM sys.tables WHERE name = ?";
  }
}
