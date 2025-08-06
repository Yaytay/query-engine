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
package uk.co.spudsoft.query.web.rest;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentValue;

/**
 *
 * @author njt
 */
public class ArgumentDetailsTest {
  
  @Test
  public void testConstructor() {
    Argument arg1 = Argument.builder().name("bob").build();
    ArgumentDetails ad = new ArgumentDetails(null, arg1);
    assertEquals("bob", ad.getName());
    
    Argument arg2 = Argument.builder().name("bob").hidden(true).build();
    assertEquals("Attempt to output hidden argument", assertThrows(IllegalStateException.class, () -> {
      new ArgumentDetails(null, arg2);
    }).getMessage());
  }
  
  @Test
  public void testDependsUpon() {
    Argument arg = Argument.builder().dependsUpon(Arrays.asList("one", "two")).build();
    ArgumentDetails ad = new ArgumentDetails(null, arg);
    assertEquals(Arrays.asList("one", "two"), ad.getDependsUpon());
  }
  
  @Test
  public void testPossibleValues() {
    Argument arg = Argument.builder().possibleValues(Arrays.asList(ArgumentValue.builder().value("one").build(), ArgumentValue.builder().value("two").build())).build();
    ArgumentDetails ad = new ArgumentDetails(null, arg);
    assertEquals(Arrays.asList(ArgumentValue.builder().value("one").build(), ArgumentValue.builder().value("two").build()), ad.getPossibleValues());
  }

}
