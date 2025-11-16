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
package uk.co.spudsoft.query.exec.filters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorMapInstance;

/**
 *
 * @author jtalbut
 */
public class WithoutFilterTest {
  
  @Test
  public void testGetKey() {
    WithoutFilter filter = new WithoutFilter();
    assertEquals("_without", filter.getKey());
  }

  @Test
  public void testCreateProcessor() {
    WithoutFilter filter = new WithoutFilter();
    ProcessorInstance instance = filter.createProcessor(null, null, null, null, "Bob", "name");
    assertNotNull(instance);
    assertThat(instance, instanceOf(ProcessorMapInstance.class));
    ProcessorMapInstance mapInstance = (ProcessorMapInstance) instance;
    assertEquals("name", mapInstance.getName());
  }
  
}
