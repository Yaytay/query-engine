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
package uk.co.spudsoft.query.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorScriptTest {

  @Test
  public void testSetType() {
    ProcessorScript instance = ProcessorScript.builder().type(ProcessorType.SCRIPT).build();
    assertEquals(ProcessorType.SCRIPT, instance.getType());
    try {
      ProcessorScript.builder().type(ProcessorType.GROUP_CONCAT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testLanguage() {
    ProcessorScript ps = ProcessorScript
      .builder()
      .type(ProcessorType.SCRIPT)
      .language("Noop")
      .build();
    assertEquals("Noop", ps.getLanguage());
  }

  @Test
  public void testPredicate() {
    ProcessorScript ps = ProcessorScript
      .builder()
      .type(ProcessorType.SCRIPT)
      .predicate("TRUE")
      .build();
    assertEquals("TRUE", ps.getPredicate());
  }

  @Test
  public void testProcess() {
    ProcessorScript ps = ProcessorScript
      .builder()
      .type(ProcessorType.SCRIPT)
      .process("x = 5")
      .build();
    assertEquals("x = 5", ps.getProcess());
  }


}
