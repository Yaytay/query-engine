/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.pipeline;

import uk.co.spudsoft.query.web.ServiceException;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.web.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 *
 * @author jtalbut
 */
public class PipelineCacheTest {
  
  private void assertPathBad(String badPath) {
    try {
      PipelineDefnLoader.validatePath(badPath);
      fail("Expected " + badPath + " to throw");
    } catch (ServiceException ex) {
    }
  }
  
  private void assertFourBadPaths(String badness) {
    assertPathBad("a" + badness + "b");
    assertPathBad(badness + "ab");
    assertPathBad("ab" + badness);
    assertPathBad(badness);        
  }
  
  @Test
  public void testBadPath() throws ServiceException {
    assertEquals("a/b/c", PipelineDefnLoader.validatePath("a/b/c"));
    assertEquals("a/_/c", PipelineDefnLoader.validatePath("a/_/c"));
    assertEquals("a/b/c", PipelineDefnLoader.validatePath("a/b/c"));
    assertPathBad("a//c");
    assertPathBad(null);
    assertPathBad("/a");
    assertPathBad("");

    assertFourBadPaths(".");
    assertFourBadPaths("\t");
    assertFourBadPaths("\b");
    assertFourBadPaths("\r");
    assertFourBadPaths("\n");
    assertFourBadPaths(Character.toString(0));
    assertFourBadPaths("<");
    assertFourBadPaths(">");
    assertFourBadPaths(":");
    assertFourBadPaths("\"");
    assertFourBadPaths("|");
    assertFourBadPaths("?");
    assertFourBadPaths("*");
  }

  @Test
  public void testGetPipeline() {
  }
  
}
