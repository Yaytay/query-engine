/*
 * Copyright (C) 2024 njt
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

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import io.vertx.core.Future;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.ProcessorQuery;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author njt
 */
public class ProcessorQueryInstanceTest {
  
  public static final RSQLParser RSQL_PARSER = new RSQLParser();
  
  @Test
  public void testInitialize() {
    ProcessorQueryInstance instance = (ProcessorQueryInstance) ProcessorQuery.builder().expression("x==4").build().createInstance(null, null, null);
    
    Future<?> initFuture = instance.initialize(null, null, null, 0);
    assertTrue(initFuture.succeeded());
  }
  
  private record Entry(String key, Comparable<?> value){};
  
  private static Entry e(String key, Comparable<?> value){
    return new Entry(key, value);
  };
  
  private static DataRow row(Entry... entries) {
    Types types = new Types();
    DataRow output = DataRow.create(types);
    for (Entry entry : entries) {
      output.put(entry.key, entry.value);
    }
    return output;
  }
  
  private static Node parse(String exp) {
    return RSQL_PARSER.parse(exp);
  }
  
  @Test
  public void testEvaluate() {
    RsqlEvaluator eval = new RsqlEvaluator();
    assertTrue(parse("x==4").accept(eval, row(e("x", 4))));
    assertFalse(parse("x==4").accept(eval, row(e("x", 5))));
    assertFalse(parse("x==4").accept(eval, row(e("x", null))));
    assertThrows(IllegalArgumentException.class, () -> {
      parse("y==4").accept(eval, row(e("x", 4)));
    }, "The field y is not present in the row: [x]");
    assertTrue(parse("x=gt=3").accept(eval, row(e("x", 4))));
    assertFalse(parse("x=gt=3").accept(eval, row(e("x", 3))));
    assertFalse(parse("x=gt=3").accept(eval, row(e("x", 2))));
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", 4))));
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", 3))));
    assertFalse(parse("x=ge=3").accept(eval, row(e("x", 2))));

    assertFalse(parse("x!=4").accept(eval, row(e("x", 4))));
    assertTrue(parse("x!=4").accept(eval, row(e("x", 5))));
    assertFalse(parse("x!=4").accept(eval, row(e("x", null))));

    // Doing string comparisons, the fact they are numeric is irrelevant
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", "4"))));
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", "3"))));
    assertFalse(parse("x=ge=3").accept(eval, row(e("x", "2"))));

    // Date/time comparisons with date/times
    assertTrue(parse("x=ge=2024-05-05T12:34").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertTrue(parse("x=ge=2024-05-06T12:34").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertFalse(parse("x=ge=2024-05-07T12:34").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));

    // Date/time comparisons with dates
    assertTrue(parse("x=ge=2024-05-05").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertTrue(parse("x=ge=2024-05-06").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertFalse(parse("x=ge=2024-05-07").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));

    // Date comparisons with dates
    assertTrue(parse("x=ge=2024-05-05").accept(eval, row(e("x", LocalDate.parse("2024-05-06")))));
    assertTrue(parse("x=ge=2024-05-06").accept(eval, row(e("x", LocalDate.parse("2024-05-06")))));
    assertFalse(parse("x=ge=2024-05-07").accept(eval, row(e("x", LocalDate.parse("2024-05-06")))));

    // Date comparisons with date/times
    assertTrue(parse("x=ge=2024-05-05").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertTrue(parse("x=ge=2024-05-06").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));
    assertFalse(parse("x=ge=2024-05-07").accept(eval, row(e("x", LocalDateTime.parse("2024-05-06T12:34")))));

    // Time comparisons with times
    assertTrue(parse("x=ge=12:34").accept(eval, row(e("x", LocalTime.parse("12:35")))));
    assertTrue(parse("x=ge=12:34").accept(eval, row(e("x", LocalTime.parse("12:34")))));
    assertFalse(parse("x=ge=12:34").accept(eval, row(e("x", LocalTime.parse("12:33")))));

    // Long comparisons
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", 4L))));
    assertTrue(parse("x=ge=3").accept(eval, row(e("x", 3L))));
    assertFalse(parse("x=ge=3").accept(eval, row(e("x", 2L))));

    // Floats comparisons (avoid equals comparisons with floats and doubles)
    assertFalse(parse("x=lt=1.3").accept(eval, row(e("x", 1.4f))));
    assertTrue(parse("x=lt=1.3").accept(eval, row(e("x", 1.2f))));
    
    // Doubles comparisons (avoid equals comparisons with floats and doubles)
    assertFalse(parse("x=le=1.3").accept(eval, row(e("x", 1.4))));
    assertTrue(parse("x=le=1.3").accept(eval, row(e("x", 1.2))));

    // Boolean comparisons (avoid equals comparisons with floats and doubles)
    assertFalse(parse("x==true").accept(eval, row(e("x", Boolean.FALSE))));
    assertTrue(parse("x==false").accept(eval, row(e("x", Boolean.FALSE))));
    
    // And
    assertTrue(parse("x=le=1.5;y==bob").accept(eval, row(e("x", 1.4), e("y", "bob"))));
    assertFalse(parse("x=le=1.5;y==bob").accept(eval, row(e("x", 1.6), e("y", "bob"))));
    assertFalse(parse("x=le=1.5;y==bob").accept(eval, row(e("x", 1.4), e("y", "fred"))));

    // Or
    assertTrue(parse("x=le=1.5,y==fred").accept(eval, row(e("x", 1.4), e("y", "bob"))));
    assertTrue(parse("x=le=1.1,y==bob").accept(eval, row(e("x", 1.2), e("y", "bob"))));
    assertFalse(parse("x=le=1.1,y==fred").accept(eval, row(e("x", 1.2), e("y", "bob"))));
    
    // Unknown operator
    RSQLParserException ex = assertThrows(RSQLParserException.class, () -> {
      parse("x=approx=3").accept(eval, row(e("x", "3")));
    });
    assertEquals("Unknown operator: =approx=", ex.getCause().getMessage());
    
    // In
    assertTrue(parse("x=in=(a,b,c,d)").accept(eval, row(e("x", "c"))));
    assertFalse(parse("x=in=(a,b,c,d)").accept(eval, row(e("x", "e"))));

    // Not In
    assertFalse(parse("x=out=(a,b,c,d)").accept(eval, row(e("x", "c"))));
    assertTrue(parse("x=out=(a,b,c,d)").accept(eval, row(e("x", "e"))));
    
  }

}
