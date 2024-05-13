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

import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Interface for a class to prepare a database with Query Engine sample data.
 * <p>
 * The majority of the work in preparing the database is handled by a database-platform-specific script file,
 * but there are some platform specific actions that mean that different platforms require slightly different handling.
 * 
 * @author jtalbut
 */
public interface SampleDataLoader {
  
  /**
   * Get the name of the same data loader.
   * @return the name of the same data loader.
   */
  String getName();
  
  /**
   * Get the identifier character to use around each identifier in the script.
   * @return the identifier character to use around each identifier in the script.
   */
  String getIdentifierQuote();
  
  /**
   * Asynchronous method to do whatever is required to prepare the test databases.
   * 
   * @param vertx Vertx instance.
   * @param url Vertx database URL (not JDBC URL).
   * @param username Username for accessing the database.
   * @param password Password for accessing the database.
   * @return A Future that will be completed when the database has been prepared.
   */
  Future<Void> prepareTestDatabase(Vertx vertx, String url, String username, String password);
  
}
