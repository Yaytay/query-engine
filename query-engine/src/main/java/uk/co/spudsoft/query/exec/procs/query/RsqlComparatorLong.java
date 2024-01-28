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
 *
 * @author jtalbut
 */
public class RsqlComparatorLong extends AbstractRsqlComparator<Long> {
  
  @Override
  public Long validateType(String field, Object value) {
    if (value instanceof Long boolValue) {
      return boolValue;
    } else {
      throw new IllegalStateException("Type of field " + field + " should be Long, but was actually " + value.getClass());
    }
  }

  @Override
  public Long parseType(String field, String value) {
    return Long.valueOf(value);
  }

}
