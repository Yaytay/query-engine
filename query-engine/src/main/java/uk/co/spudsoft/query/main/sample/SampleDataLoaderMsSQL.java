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
 * Sample data load for Microsoft SQL Server.
 *
 * @author jtalbut
 */
public class SampleDataLoaderMsSQL extends AbstractSampleDataLoader {

  /**
   * Constructor.
   * @param basePath The root directory for storing lock files.
   */
  public SampleDataLoaderMsSQL(String basePath) {
    super(basePath);
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
  public String getIdentifierQuote() {
    return "[";
  }
}
