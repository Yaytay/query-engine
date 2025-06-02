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
 * Specialism of {@link RsqlComparator} for String values.
 *
 * @author jtalbut
 */
public class RsqlComparatorString extends AbstractRsqlComparator<String> {

  /**
   * Constructor.
   */
  public RsqlComparatorString() {
  }
  
  
  @Override
  public String validateType(String field, Object value) {
    if (value instanceof String stringValue) {
      return stringValue;
    } else {
      throw new IllegalStateException("Type of field " + field + " should be String, but was actually " + value.getClass());
    }
  }

  @Override
  public String parseType(String field, String value) {
    return value;
  }

  @Override
  public boolean equal(String rowValue, String compareValue) {
    if (rowValue == null) {
      return compareValue == null;
    } else if (compareValue == null) {
      return false;
    }
    if (compareValue.startsWith("*")) {
      if (compareValue.endsWith("*")) {
        return rowValue.contains(compareValue.substring(1, compareValue.length() - 1));
      } else {
        return rowValue.endsWith(compareValue.substring(1, compareValue.length()));
      }
    } else if (compareValue.endsWith("*")) {
      return rowValue.startsWith(compareValue.substring(0, compareValue.length() - 1));
    } else {
      return super.equal(rowValue, compareValue);
    } 
  }

  
  
}
