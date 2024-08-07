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
package uk.co.spudsoft.query.exec.sources.sql;

/**
 * SQL preparer for MySQL.
 *
 * @author jtalbut
 */
public class MySqlPreparer extends AbstractSqlPreparer {

  /**
   * Constructor.
   */
  public MySqlPreparer() {
  }
  
  @Override
  void generateParameterNumber(StringBuilder builder, int number) {
    builder.append("?");
  }

  @Override
  boolean hasNumberedParameters() {
    return false;
  }

  @Override
  protected String getQuoteCharacter() {
    return "`";
  }
  
}
