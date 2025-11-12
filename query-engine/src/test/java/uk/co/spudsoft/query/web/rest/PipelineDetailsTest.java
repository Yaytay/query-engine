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

import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentGroup;
import uk.co.spudsoft.query.defn.FormatDelimited;

/**
 *
 * @author jtalbut
 */
public class PipelineDetailsTest {
  
  @Test
  public void testConstructor() {
    PipelineDetails pd = new PipelineDetails(null, null, null, null, null, null, null, null);
    assertNull(pd.getArgumentGroups());
    assertNull(pd.getArguments());
    assertNull(pd.getFormats());
    
    pd = new PipelineDetails(null, null, null, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    assertNull(pd.getArgumentGroups());
    assertNull(pd.getArguments());
    assertNull(pd.getFormats());
    
    pd = new PipelineDetails(null, null, null, null, null
            , Arrays.asList(
                    ArgumentGroup.builder().name("group1").build()
            )
            , Arrays.asList(
                    Argument.builder().name("arg1").build()
            )
            , Arrays.asList(
                    FormatDelimited.builder().name("fmt").build()
            )
    );
    assertNotNull(pd.getArgumentGroups());
    assertEquals("group1", pd.getArgumentGroups().get(0).getName());
    assertNotNull(pd.getArguments());
    assertEquals("arg1", pd.getArguments().get(0).getName());
    assertNotNull(pd.getFormats());
    assertEquals("fmt", pd.getFormats().get(0).getName());
    
  }
}
