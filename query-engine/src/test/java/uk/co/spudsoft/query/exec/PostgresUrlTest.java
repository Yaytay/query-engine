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

import java.sql.Connection;
import java.sql.DriverManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class PostgresUrlTest {
  
  private static final Logger logger = LoggerFactory.getLogger(PostgresUrlTest.class);
  
  @Test
  public void testBadProvider() {
    String url = "jdbc:nonexistant:wibble";
    logger.info("Attempting to open: {}", url);
    try (Connection jdbcConnection = DriverManager.getConnection(url)) {
      logger.debug("Opened connection {}", jdbcConnection);
    } catch (Throwable ex) {
      logger.info("Attempt to open {} threw: ", url, ex);
      assertThat(ex.getMessage(), startsWith("No suitable driver found for"));
    }
  }
  
  @Test
  public void testBadHost() {
    String url = "jdbc:postgresql://wibble/db";
    logger.info("Attempting to open: {}", url);
    try (Connection jdbcConnection = DriverManager.getConnection(url)) {
      logger.debug("Opened connection {}", jdbcConnection);
    } catch (Throwable ex) {
      logger.info("Attempt to open {} threw: ", url, ex);
      assertThat(ex.getMessage(), startsWith("The connection attempt failed"));
    }
  }
  
}
