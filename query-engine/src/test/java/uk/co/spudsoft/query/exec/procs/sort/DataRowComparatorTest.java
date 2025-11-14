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

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
public class DataRowComparatorTest {
  
  private final Types types = new Types();
  private final DataRow rowN = DataRow.create(types);
  private final DataRow row0 = DataRow.create(types)
            .convertPut("nullValue", null)
            .convertPut("intValue", 6)
            .convertPut("longValue", 1L << 40)
            .convertPut("floatValue", 3.3f)
            .convertPut("doubleValue", 3.1415925)
            .convertPut("stringValue", "wibbla")
            .convertPut("boolValue", false)
            .convertPut("timeValue", LocalTime.of(12, 33))
            .convertPut("dateValue", LocalDate.of(1971, 05, 05))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 33))
            .convertPut("sqlDate", new java.sql.Date(42246000000L))       // 1971-05-05
            .convertPut("sqlTime", new java.sql.Time(41580000L))          // 12:33:00
            .convertPut("javaDate", new java.util.Date(42381239999L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "helli").build())
            ;
  private final DataRow row10 = DataRow.create(types)
            .convertPut("nullValue", null)
            .convertPut("intValue", 7)
            .convertPut("longValue", 3L << 40)
            .convertPut("floatValue", 3.3f)
            .convertPut("doubleValue", 3.1415925)
            .convertPut("stringValue", "wibbla")
            .convertPut("boolValue", false)
            .convertPut("timeValue", LocalTime.of(12, 33))
            .convertPut("dateValue", LocalDate.of(1971, 05, 05))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 33))
            .convertPut("sqlDate", new java.sql.Date(42332400000L))       // 1971-05-06
            .convertPut("sqlTime", new java.sql.Time(41640000L))          // 12:34
            .convertPut("javaDate", new java.util.Date(42381239999L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "helli").build())
            ;
  private final DataRow row1 = DataRow.create(types)
            .convertPut("nullValue", null)
            .convertPut("intValue", 7)
            .convertPut("longValue", 3L << 40)
            .convertPut("floatValue", 3.4f)
            .convertPut("doubleValue", 3.1415926)
            .convertPut("stringValue", "wibble")
            .convertPut("boolValue", false)
            .convertPut("timeValue", LocalTime.of(12, 34))
            .convertPut("dateValue", LocalDate.of(1971, 05, 06))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 34))
            .convertPut("sqlDate", new java.sql.Date(42332400000L))       // 1971-05-06
            .convertPut("sqlTime", new java.sql.Time(41640000L))          // 12:34
            .convertPut("javaDate", new java.util.Date(42381240000L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "hello").build())
            ;
  private final DataRow row12 = DataRow.create(types)
            .convertPut("nullValue", null)
            .convertPut("intValue", 7)
            .convertPut("longValue", 3L << 40)
            .convertPut("floatValue", 3.5f)
            .convertPut("doubleValue", 3.1415925)
            .convertPut("stringValue", "wibbla")
            .convertPut("boolValue", false)
            .convertPut("timeValue", LocalTime.of(12, 33))
            .convertPut("dateValue", LocalDate.of(1971, 05, 05))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 33))
            .convertPut("sqlDate", new java.sql.Date(42332400000L))       // 1971-05-06
            .convertPut("sqlTime", new java.sql.Time(41640000L))          // 12:34
            .convertPut("javaDate", new java.util.Date(42381239999L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "helli").build())
            ;
  private final DataRow row2 = DataRow.create(types)
            .convertPut("nullValue", null)
            .convertPut("intValue", 8)
            .convertPut("longValue", 5L << 40)
            .convertPut("floatValue", 3.5f)
            .convertPut("doubleValue", 3.1415927)
            .convertPut("stringValue", "wibbli")
            .convertPut("boolValue", true)
            .convertPut("timeValue", LocalTime.of(12, 35))
            .convertPut("dateValue", LocalDate.of(1971, 05, 07))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 35))
            .convertPut("sqlDate", new java.sql.Date(42418800000L))       // 1971-05-07
            .convertPut("sqlTime", new java.sql.Time(41700000L))          // 12:35
            .convertPut("javaDate", new java.util.Date(42381240001L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "hellu").build())
            ;

  
  @Test
  public void testCreateChain() {

    // Finding test values
//    java.sql.Date s =new java.sql.Date(71, 04, 05);
//    System.out.println("Number: " + s.toString() + " = " + s.getTime());
//    s =new java.sql.Date(71, 04, 06);
//    System.out.println("Number: " + s.toString() + " = " + s.getTime());
//    s =new java.sql.Date(71, 04, 07);
//    System.out.println("Number: " + s.toString() + " = " + s.getTime());
//    java.sql.Time t =new java.sql.Time(12, 33, 00);
//    System.out.println("Number: " + t.toString() + " = " + t.getTime());
//    t =new java.sql.Time(12, 34, 00);
//    System.out.println("Number: " + t.toString() + " = " + t.getTime());
//    t =new java.sql.Time(12, 35, 00);
//    System.out.println("Number: " + t.toString() + " = " + t.getTime());
    
    Comparator<DataRow> comp = new DataRowComparator(null, Arrays.asList("intValue"));
    assertEquals(-1, comp.compare(row1, row2));
    assertEquals(0, comp.compare(rowN, rowN));
    assertEquals(-1, comp.compare(rowN, row1));
    assertEquals(1, comp.compare(row1, rowN));
    
    comp = new DataRowComparator(null, Arrays.asList("-intValue"));
    assertEquals(1, comp.compare(row1, row2));
    assertEquals(0, comp.compare(rowN, rowN));
    assertEquals(1, comp.compare(rowN, row1));
    assertEquals(-1, comp.compare(row1, rowN));
    
    comp = new DataRowComparator(null, Arrays.asList("intValue", "longValue"));
    assertEquals(0, comp.compare(row1, row12));
    assertEquals(0, comp.compare(row1, row10));
    comp = new DataRowComparator(null, Arrays.asList("intValue", "longValue", "floatValue"));
    assertEquals(-1, comp.compare(row1, row12));
    assertEquals(1, comp.compare(row1, row10));
    comp = new DataRowComparator(null, Arrays.asList("intValue", "-longValue", "floatValue"));
    assertEquals(-1, comp.compare(row1, row12));
    assertEquals(1, comp.compare(row1, row10));
    comp = new DataRowComparator(null, Arrays.asList("intValue", "longValue", "-floatValue"));
    assertEquals(1, comp.compare(row1, row12));
    assertEquals(-1, comp.compare(row1, row10));
    
    comp = new DataRowComparator(null, Arrays.asList("doubleValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("stringValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("timeValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("dateValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("dateTimeValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("sqlDate"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("sqlTime"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    comp = new DataRowComparator(null, Arrays.asList("javaDate"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
    
    comp = new DataRowComparator(null, Arrays.asList("boolValue"));
    assertThat(comp.compare(row1, row2), lessThan(0));
    assertThat(comp.compare(row2, row1), greaterThan(0));
    assertEquals(0, comp.compare(rowN, rowN));
    assertThat(comp.compare(rowN, row1), lessThan(0));
    assertThat(comp.compare(row1, rowN), greaterThan(0));
    assertThat(comp.compare(rowN, row2), lessThan(0));
    assertThat(comp.compare(row2, rowN), greaterThan(0));
    
  }
  
}
