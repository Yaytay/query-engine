/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.procs.query;

/**
 * Specialism of {@link RsqlComparator} for Double values.
 *
 * @author jtalbut
 */
public class RsqlComparatorDouble extends AbstractRsqlComparator<Double> {

  /**
   * Constructor.
   */
  public RsqlComparatorDouble() {
  }
  
  
  @Override
  public Double validateType(String field, Object value) {
    if (value instanceof Double boolValue) {
      return boolValue;
    } else {
      throw new IllegalStateException("Type of field " + field + " should be Double, but was actually " + value.getClass());
    }
  }

  @Override
  public Double parseType(String field, String value) {
    return Double.valueOf(value);
  }

}
