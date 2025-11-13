/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.exec.fmts;

import inet.ipaddr.IPAddressString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 *
 * @author jtalbut
 */
public class CustomBooleanFormatterTest {
  
  private static final Logger logger = LoggerFactory.getLogger(CustomBooleanFormatterTest.class);
  
  @Test
  public void testFormatValidExpressionBad() {
    assertEquals("Unable to parse \"...\" as a JEXL expression: parsing error in '...'", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("...", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatValidExpressionNotArrays() {
    assertEquals("Expression must evaluate to a two-element array of strings: 1", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("1", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatTwoStringsNotValid() {
    assertEquals("The true value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['bob', 'fred']", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatQuotingBad() {
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', 'fred']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', '\"fred']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', 'fred\"']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['true', 'False']", "<", ">", true);
    }).getMessage());
    new CustomBooleanFormatter("['true', 'False']", "<", ">", false);
  }
  
  @Test
  public void testFormatValid() {
    CustomBooleanFormatter formatter = new CustomBooleanFormatter("['\"1\"', '\"0\"']", "\"", "\"", true);
    
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    
    assertNull(formatter.format(pipelineContext, null));
    
    assertEquals("\"1\"", formatter.format(pipelineContext, Boolean.TRUE));
    assertEquals("\"0\"", formatter.format(pipelineContext, Boolean.FALSE));

    formatter = new CustomBooleanFormatter("['true', 'false']", "<", ">", true);

    assertEquals("true", formatter.format(pipelineContext, 1));
    assertEquals("false", formatter.format(pipelineContext, 0));

    assertEquals("true", formatter.format(pipelineContext, "true"));
    assertEquals("false", formatter.format(pipelineContext, "not true"));

    formatter = new CustomBooleanFormatter("['true', 'fALSe']", "<", ">", false);

    assertEquals("true", formatter.format(pipelineContext, 1));
    assertEquals("fALSe", formatter.format(pipelineContext, 0));

    formatter = new CustomBooleanFormatter("['1', '0']", "<", ">", true);

    assertEquals("1", formatter.format(pipelineContext, 1));
    assertEquals("0", formatter.format(pipelineContext, 0));

    assertEquals("1", formatter.format(pipelineContext, "true"));
    assertEquals("0", formatter.format(pipelineContext, "not true"));
  }
  
  
}
