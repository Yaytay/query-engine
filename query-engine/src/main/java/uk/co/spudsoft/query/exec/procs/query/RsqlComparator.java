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

import java.util.Set;

/**
 * Interface representing a comparison that can be carried out in RSQL (or FIQL).
 * <P>
 * Specialisms should exist for specific types.
 * 
 * @param <T> the java type that this comparator handles
 * @author jtalbut
 */
public interface RsqlComparator<T> {

  /**
   * Validate that the value is of type T.
   * @param field The name of the field being validated.
   * @param value The value to validate.
   * @return The value cast to type T.
   * @throws IllegalStateException if the value is not of the appropriate type.
   */
  T validateType(String field, Object value) throws IllegalStateException;
  
  /**
   * Parse a string value to type T.
   * @param field The name of the field being parsed.
   * @param value The string representation of the value.
   * @return The value parsed to type T.
   */
  T parseType(String field, String value);
  
  
  /**
   * Compare two values of type T and return true if they are equal.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue == compareValue.
   */
  boolean equal(T rowValue, T compareValue);
  /**
   * Compare two values of type T and return true if they are not equal.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue != compareValue.
   */
  boolean notEqual(T rowValue, T compareValue);
  /**
   * Compare two values of type T and return true if rowValue is greater than compareValue.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue &gt; compareValue.
   */
  boolean greaterThan(T rowValue, T compareValue);
  /**
   * Compare two values of type T and return true if rowValue is greater than or equal to compareValue.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue &gt;= compareValue.
   */
  boolean greaterThanOrEqual(T rowValue, T compareValue);
  /**
   * Compare two values of type T and return true if rowValue is less than compareValue.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue &lt; compareValue.
   */
  boolean lessThan(T rowValue, T compareValue);
  /**
   * Compare two values of type T and return true if rowValue is less than or equal to compareValue.
   * @param rowValue The left value to compare.
   * @param compareValue The right value to compare.
   * @return true if rowValue &lt;= compareValue.
   */
  boolean lessThanOrEqual(T rowValue, T compareValue);
  
  /**
   * Return true if rowValue is in the {@link java.util.Set} compareValue.
   * @param rowValue The value to search for.
   * @param compareValue The set of values to search in.
   * @return true if rowValue is in the {@link java.util.Set} compareValue.
   */
  boolean in(T rowValue, Set<T> compareValue);
  /**
   * Return true if rowValue is not in the {@link java.util.Set} compareValue.
   * @param rowValue The value to search for.
   * @param compareValue The set of values to search in.
   * @return true if rowValue is not in the {@link java.util.Set} compareValue.
   */
  boolean notIn(T rowValue, Set<T> compareValue);
  
}
