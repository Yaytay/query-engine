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

/**
 * Sample data load for PostgreSQL.
 *
 * @author jtalbut
 */
public class SampleDataLoaderPostgreSQL extends AbstractSampleDataLoader {

  /**
   * Constructor.
   */
  public SampleDataLoaderPostgreSQL() {
  }

  @Override
  public String getName() {
    return "PostgreSQL";
  }

  @Override
  protected String getScript() {
    return "/sampleData/PostgreSQL Test Structures.sql";
  }

  @Override
  public String getIdentifierQuote() {
    return "\"";
  }
}
