/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.main;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.slf4j.Logger;

/**
 *
 * @author jtalbut
 */
public class TestHelpers {
  
  public static JsonArray getDirtyAudits(Logger logger, String jdbcUrl, String username, String password) throws SQLException {

    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, username, password); java.sql.Statement stmt = conn.createStatement()) {

      // Query for requests without response_timeout
      JsonArray rows = new JsonArray();
      String query = "select * from request where responseTime is null";
      if (jdbcUrl.contains("postgres")) {
        query = "select * from \"public\".\"request\" where \"responseTime\" is null";
      }
      try (java.sql.ResultSet rs = stmt.executeQuery(query)) {
        ResultSetMetaData meta = rs.getMetaData();

        while (rs.next()) {
          JsonObject row = new JsonObject();
          for (int i = 1; i <= meta.getColumnCount(); ++i) {
            row.put(meta.getColumnName(i), rs.getObject(i));
          }
          rows.add(row);
        }
      }
      if (!rows.isEmpty()) {
        logger.info("Dirty audit rows: {}", rows);
      }
      return rows;
    } catch (Throwable ex) {
      logger.warn("Failed to get audit rows: {}", ex);
      return new JsonArray();
    }
  }
  
}
