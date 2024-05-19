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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * Specialism of {@link RsqlComparator} for Boolean values.
 * <P>
 * Cannot use AbstractRsqlComparator due to the nature of the LocalDateTime class.
 *
 * @author jtalbut
 */
public class RsqlComparatorDateTime implements RsqlComparator<LocalDateTime> {

  /**
   * Constructor.
   */
  public RsqlComparatorDateTime() {
  }
  
  
  @Override
  public LocalDateTime validateType(String field, Object value) {
    if (value instanceof LocalDateTime boolValue) {
      return boolValue;
    } else {
      throw new IllegalStateException("Type of field " + field + " should be LocalDateTime, but was actually " + value.getClass());
    }
  }

  @Override
  public LocalDateTime parseType(String field, String value) {
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException ex) {
      try {
        return LocalDate.parse(value).atStartOfDay();
      } catch (DateTimeParseException ex2) {
        throw ex;
      }
    }
  }

  @Override
  public boolean equal(LocalDateTime rowValue, LocalDateTime compareValue) {
    return Objects.equals(rowValue, compareValue);
  }

  @Override
  public boolean notEqual(LocalDateTime rowValue, LocalDateTime compareValue) {
    return !Objects.equals(rowValue, compareValue);
  }

  @Override
  public boolean greaterThan(LocalDateTime rowValue, LocalDateTime compareValue) {
    return Objects.compare(rowValue, compareValue, Comparator.naturalOrder()) > 0;
  }

  @Override
  public boolean greaterThanOrEqual(LocalDateTime rowValue, LocalDateTime compareValue) {
    return equal(rowValue, compareValue) || greaterThan(rowValue, compareValue);
  }

  @Override
  public boolean lessThan(LocalDateTime rowValue, LocalDateTime compareValue) {
    return Objects.compare(rowValue, compareValue, Comparator.naturalOrder()) < 0;
  }

  @Override
  public boolean lessThanOrEqual(LocalDateTime rowValue, LocalDateTime compareValue) {
    return equal(rowValue, compareValue) || lessThan(rowValue, compareValue);
  }

  @Override
  public boolean in(LocalDateTime rowValue, Set<LocalDateTime> compareValue) {
    return compareValue.contains(rowValue);
  }

  @Override
  public boolean notIn(LocalDateTime rowValue, Set<LocalDateTime> compareValue) {
    return !compareValue.contains(rowValue);
  }
  
  
}
