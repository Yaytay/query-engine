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

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract implementation of the {@link RsqlComparator} interface, using methods from {@link java.util.Objects} for the comparisons.
 * @param <T> The type be compared
 * @author jtalbut
 */
public abstract class AbstractRsqlComparator<T extends Comparable<T>> implements RsqlComparator<T> {
  
  @Override
  public boolean equal(T rowValue, T compareValue) {
    return Objects.equals(rowValue, compareValue);
  }

  @Override
  public boolean notEqual(T rowValue, T compareValue) {
    return !Objects.equals(rowValue, compareValue);
  }

  @Override
  public boolean greaterThan(T rowValue, T compareValue) {
    return Objects.compare(rowValue, compareValue, Comparator.naturalOrder()) > 0;
  }

  @Override
  public boolean greaterThanOrEqual(T rowValue, T compareValue) {
    return !lessThan(rowValue, compareValue);
  }

  @Override
  public boolean lessThan(T rowValue, T compareValue) {
    return Objects.compare(rowValue, compareValue, Comparator.naturalOrder()) < 0;
  }

  @Override
  public boolean lessThanOrEqual(T rowValue, T compareValue) {
    return !greaterThan(rowValue, compareValue);
  }

  @Override
  public boolean in(T rowValue, Set<T> compareValue) {
    return compareValue.contains(rowValue);
  }

  @Override
  public boolean notIn(T rowValue, Set<T> compareValue) {
    return !compareValue.contains(rowValue);
  }
  
  
}
