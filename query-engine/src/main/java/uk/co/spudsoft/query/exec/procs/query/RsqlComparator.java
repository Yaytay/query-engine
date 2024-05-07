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

  T validateType(String field, Object value);
  
  T parseType(String field, String value);
  
  boolean equal(T rowValue, T compareValue);
  boolean notEqual(T rowValue, T compareValue);
  boolean greaterThan(T rowValue, T compareValue);
  boolean greaterThanOrEqual(T rowValue, T compareValue);
  boolean lessThan(T rowValue, T compareValue);
  boolean lessThanOrEqual(T rowValue, T compareValue);
  
  boolean in(T rowValue, Set<T> compareValue);
  boolean notIn(T rowValue, Set<T> compareValue);
  
}
