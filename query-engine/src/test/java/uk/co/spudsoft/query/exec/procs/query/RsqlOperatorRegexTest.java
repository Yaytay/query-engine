/*
 * Copyright (C) 2025 njt
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
import java.util.ArrayList;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author njt
 */
public class RsqlOperatorRegexTest {
  
  @Test
  public void testCompare() {
    
    RsqlOperatorRegex op = new RsqlOperatorRegex();
    
    assertThat(assertThrows(IllegalArgumentException.class, () -> {
      op.compare("rowValue", Arrays.asList());
    }).getMessage(), startsWith("0 arguments"));
    
    assertThat(assertThrows(IllegalArgumentException.class, () -> {
      op.compare("rowValue", Arrays.asList("one", "two"));
    }).getMessage(), startsWith("2 arguments"));
    
    assertThat(assertThrows(IllegalArgumentException.class, () -> {
      op.compare("rowValue", Arrays.asList("([)]"));
    }).getMessage(), startsWith("Invalid argument passed to =~ operator"));
    
    assertTrue(op.compare("rowValue", Arrays.asList(".*V.*")));
    assertFalse(op.compare("rowValue", Arrays.asList(".*X.*")));
  }
  
  @Test
  public void testOperate() {

    RsqlOperatorRegex op = new RsqlOperatorRegex();
    
    assertThat(assertThrows(AssertionError.class, () -> {
      op.operate("field", new RsqlComparatorString(), "one", Arrays.asList("one"));
    }).getMessage(), startsWith("rsqlComparator must be null"));
    
    assertFalse(op.operate("field", null, null, Arrays.asList("one")));
    ArrayList<String> nullArg = new ArrayList<>();
    nullArg.add(null);
    assertFalse(op.operate("field", null, null, nullArg));
    assertTrue(op.operate("field", null, "row", Arrays.asList("row")));
    
  }
  
}
