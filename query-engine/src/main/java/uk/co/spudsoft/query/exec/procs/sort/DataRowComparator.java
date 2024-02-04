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
package uk.co.spudsoft.query.exec.procs.sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
public class DataRowComparator {
  
  private static final Logger logger = LoggerFactory.getLogger(DataRowComparator.class);
  
  public static Comparator<DataRow> createChain(Types types, List<String> fields) {
    Comparator<DataRow> result = null;
    for (String field : fields) {
      boolean descending = false;
      if (field.startsWith("-")) {
        descending = true;
        field = field.substring(1);
      }
      DataType type = types.get(field);
      if (type == null) {
        logger.warn("Field {} not found in fields {}", field, types.keySet());
        throw new IllegalArgumentException("Unrecognised field in sort");
      }
      Comparator<DataRow> comparator = switch (type) {
        case Boolean -> { 
          yield new BooleanKey(field, descending); 
        }
        case Date -> { 
          yield new LocalDateKey(field, descending); 
        }
        case DateTime -> { 
          yield new LocalDateTimeKey(field, descending); 
        }
        case Double -> { 
          yield new DoubleKey(field, descending); 
        }
        case Float -> { 
          yield new FloatKey(field, descending); 
        }
        case Integer -> { 
          yield new IntegerKey(field, descending); 
        }
        case Long -> { 
          yield new LongKey(field, descending); 
        }
        case String -> { 
          yield new StringKey(field, descending); 
        }
        case Time -> { 
          yield new LocalTimeKey(field, descending); 
        }
        default -> { 
          throw new IllegalStateException("Unable to create comparator for field of type " + type); 
        }
      };
      if (result == null) {
        result = comparator;
      } else {
        result = result.thenComparing(comparator);
      }
    }
    return result;
  }

  
  public static class BooleanKey extends AbstractDataRowFieldComparator<Boolean> {

    private static final long serialVersionUID = 1L;

    public BooleanKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(Boolean v1, Boolean v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class IntegerKey extends AbstractDataRowFieldComparator<Integer> {

    private static final long serialVersionUID = 1L;

    public IntegerKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(Integer v1, Integer v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class LongKey extends AbstractDataRowFieldComparator<Long> {

    private static final long serialVersionUID = 1L;

    public LongKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(Long v1, Long v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class FloatKey extends AbstractDataRowFieldComparator<Float> {

    private static final long serialVersionUID = 1L;

    public FloatKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(Float v1, Float v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class DoubleKey extends AbstractDataRowFieldComparator<Double> {

    private static final long serialVersionUID = 1L;

    public DoubleKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(Double v1, Double v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class StringKey extends AbstractDataRowFieldComparator<String> {

    private static final long serialVersionUID = 1L;

    public StringKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(String v1, String v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class LocalDateKey extends AbstractDataRowFieldComparator<ChronoLocalDate> {

    private static final long serialVersionUID = 1L;

    public LocalDateKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(ChronoLocalDate v1, ChronoLocalDate v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class LocalDateTimeKey extends AbstractDataRowFieldComparator<ChronoLocalDateTime<LocalDate>> {

    private static final long serialVersionUID = 1L;

    public LocalDateTimeKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(ChronoLocalDateTime<LocalDate> v1, ChronoLocalDateTime<LocalDate> v2) {
      return v1.compareTo(v2);
    }

  }
  
  public static class LocalTimeKey extends AbstractDataRowFieldComparator<LocalTime> {

    private static final long serialVersionUID = 1L;

    public LocalTimeKey(String field, boolean descending) {
      super(field, descending);
    }

    @Override
    protected int internalCompare(LocalTime v1, LocalTime v2) {
      return v1.compareTo(v2);
    }

  }
  
}
