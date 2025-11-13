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

import inet.ipaddr.IPAddressString;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.web.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import uk.co.spudsoft.query.exec.context.RequestContext;


/**
 *
 * @author jtalbut
 */
public class PipelineCacheTest {
  
  private void assertPathBad(String badPath) {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    
    try {
      PipelineDefnLoader.validatePath(reqctx, badPath);
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
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    assertEquals("a/b/c", PipelineDefnLoader.validatePath(reqctx, "a/b/c"));
    assertEquals("a/_/c", PipelineDefnLoader.validatePath(reqctx, "a/_/c"));
    assertEquals("a/b/c", PipelineDefnLoader.validatePath(reqctx, "a/b/c"));
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
  
}
