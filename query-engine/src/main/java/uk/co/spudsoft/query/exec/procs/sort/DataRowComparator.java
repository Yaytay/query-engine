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

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Long;
import static uk.co.spudsoft.query.defn.DataType.String;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * {@link java.util.Comparator} class for comparing {@link uk.co.spudsoft.query.exec.DataRow} objects.
 * <P>
 * The comparison will be based on the fields provided to the constructor, which may be preceded by "-" to invert the result (to do a descending sort).
 * 
 * @author jtalbut
 */
public class DataRowComparator implements Comparator<DataRow> {
  
  private static final Logger logger = LoggerFactory.getLogger(DataRowComparator.class);

  private final PipelineContext pipelineContext;

  private final ImmutableList<String> fields;
  
  /**
   * Constructor.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param fields The list of fields to use in the comparison. 
   */
  public DataRowComparator(PipelineContext pipelineContext, List<String> fields) {
    this.pipelineContext = pipelineContext;
    this.fields = ImmutableCollectionTools.copy(fields);
  }

  @Override
  public int compare(DataRow o1, DataRow o2) {
    for (String field : fields) {
      int descending = 1;
      if (field.startsWith("-")) {
        descending = -1;
        field = field.substring(1);
      }
      int result = compareField(pipelineContext, o1, o2, field) * descending;
      if (result != 0) {
        return result;
      }
    }
    return 0;    
  }
  
  private static int compareField(PipelineContext pipelineContext, DataRow o1, DataRow o2, String field) {
    if (o1 == null) {
      return (o2 == null) ? 0 : -1;
    }
    
    DataType type = o1.getType(field);
    if (type == null) {
      Log.decorate(logger.atWarn(), pipelineContext).log("Field {} not found in fields {}", field, o1.types().keySet());
      throw new IllegalArgumentException("Unrecognised field in sort");
    }
    
    return switch (type) {
        case Boolean -> { 
          yield compareBooleanFields(o1, o2, field); 
        }
        case Date -> { 
          yield compareDateFields(o1, o2, field); 
        }
        case DateTime -> { 
          yield compareDateTimeFields(o1, o2, field); 
        }
        case Double -> { 
          yield compareDoubleFields(o1, o2, field); 
        }
        case Float -> { 
          yield compareFloatFields(o1, o2, field); 
        }
        case Integer -> { 
          yield compareIntegerFields(o1, o2, field); 
        }
        case Long -> { 
          yield compareLongFields(o1, o2, field); 
        }
        case String -> { 
          yield compareStringFields(o1, o2, field); 
        }
        case Time -> { 
          yield compareTimeFields(o1, o2, field); 
        }
        default -> { 
          throw new IllegalStateException("Unable to compare fields of type " + type); 
        }
      };
  }
  
  private static int compareBooleanFields(DataRow o1, DataRow o2, String field) {
    Boolean v1 = (Boolean) o1.get(field);
    Boolean v2 = (Boolean) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareDateFields(DataRow o1, DataRow o2, String field) {
    LocalDate v1 = (LocalDate) o1.get(field);
    LocalDate v2 = (LocalDate) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareDateTimeFields(DataRow o1, DataRow o2, String field) {
    LocalDateTime v1 = (LocalDateTime) o1.get(field);
    LocalDateTime v2 = (LocalDateTime) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareDoubleFields(DataRow o1, DataRow o2, String field) {
    Double v1 = (Double) o1.get(field);
    Double v2 = (Double) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareFloatFields(DataRow o1, DataRow o2, String field) {
    Float v1 = (Float) o1.get(field);
    Float v2 = (Float) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareIntegerFields(DataRow o1, DataRow o2, String field) {
    Integer v1 = (Integer) o1.get(field);
    Integer v2 = (Integer) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareLongFields(DataRow o1, DataRow o2, String field) {
    Long v1 = (Long) o1.get(field);
    Long v2 = (Long) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareStringFields(DataRow o1, DataRow o2, String field) {
    String v1 = (String) o1.get(field);
    String v2 = (String) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
  
  private static int compareTimeFields(DataRow o1, DataRow o2, String field) {
    LocalTime v1 = (LocalTime) o1.get(field);
    LocalTime v2 = (LocalTime) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : -1;
    } else if (v2 == null) {
      return 1;
    } else {
      return v1.compareTo(v2);
    }
  }
}
