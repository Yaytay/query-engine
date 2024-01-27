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
package uk.co.spudsoft.query.defn;

import java.util.Arrays;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorWithoutInstance;

/**
 *
 * @author njt
 */
public class ProcessorWithoutTest {
  
  @Test
  public void testCreateInstance() {
    assertThat(ProcessorWithout.builder().build().createInstance(null, null, null), instanceOf(ProcessorWithoutInstance.class));
  }

  @Test
  public void testValidate() {
    ProcessorWithout.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorWithout.builder().type(ProcessorType.LIMIT).build().validate();
    });
  }

  @Test
  public void testGetType() {
    assertEquals(ProcessorType.WITHOUT, ProcessorWithout.builder().build().getType());
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorWithout.builder().type(ProcessorType.LIMIT).build();
    });
  }

  @Test
  public void testGetCondition() {
    assertNull(ProcessorWithout.builder().build().getCondition());
    assertEquals("true", ProcessorWithout.builder().condition(new Condition("true")).build().getCondition().getExpression());
  }

  @Test
  public void testGetFields() {
    assertThat(ProcessorWithout.builder().build().getFields(), instanceOf(List.class));
    assertEquals(Arrays.asList("a", "b"), ProcessorWithout.builder().fields(Arrays.asList("a", "b")).build().getFields());
  }
  
}
