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
package uk.co.spudsoft.query.exec.procs.subquery;

import java.util.Arrays;
import java.util.List;
import uk.co.spudsoft.query.defn.ProcessorMerge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
public class ProcessorMergeInstanceTest {
  
  @Test
  public void testGetId() {
    ProcessorMerge defn = ProcessorMerge.builder().name("id").build();
    ProcessorMergeInstance instance = new ProcessorMergeInstance(null, null, null, defn, "P0-Merge");
    assertEquals("P0-Merge", instance.getName());
  }

  @Test
  public void testProcessChildren() {
    ProcessorMerge defn = ProcessorMerge.builder()
            .parentIdColumns(Arrays.asList("id"))
            .childIdColumns(Arrays.asList("id"))
            .build();
    ProcessorMergeInstance instance = new ProcessorMergeInstance(null, null, null, defn, "P0-Merge");
    
    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");
    
    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("value", 1)
            , DataRow.create(childTypes).put("value", null)
            , DataRow.create(childTypes)
            , DataRow.create(childTypes).put("value", 3)
    );
    
    instance.setChildTypes(childTypes);
    DataRow result = instance.processChildren(parent, children);
    assertEquals(2, result.size());
    assertEquals("one", result.get("id"));
    assertEquals(1, result.get("value"));
  }
  
}
