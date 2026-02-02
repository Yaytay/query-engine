/*
 * Copyright (C) 2026 jtalbut
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
package uk.co.spudsoft.query.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Condition;

/**
 * Unit tests for {@link Operators}.
 */
public class OperatorsTest {

  private Operators operators;

  @BeforeEach
  void setUp() {
    operators = new Operators();
  }

  @Test
  void testGetSetGlobal() {
    Condition globalCondition = mock(Condition.class);
    assertNull(operators.getGlobal());

    operators.setGlobal(globalCondition);
    assertEquals(globalCondition, operators.getGlobal());
  }

  @Test
  void testGetSetClient() {
    Condition clientCondition = mock(Condition.class);
    assertNull(operators.getClient());

    operators.setClient(clientCondition);
    assertEquals(clientCondition, operators.getClient());
  }

  @Test
  void testValidateWithNullConditions() {
    // Should not throw any exception when conditions are null
    operators.validate();
  }

  @Test
  void testValidateCallsConditions() {
    Condition globalCondition = mock(Condition.class);
    Condition clientCondition = mock(Condition.class);

    operators.setGlobal(globalCondition);
    operators.setClient(clientCondition);

    operators.validate();

    verify(globalCondition, times(1)).validate();
    verify(clientCondition, times(1)).validate();
  }

  @Test
  void testValidateThrowsWhenGlobalInvalid() {
    Condition globalCondition = mock(Condition.class);
    doThrow(new IllegalArgumentException("Invalid global condition")).when(globalCondition).validate();

    operators.setGlobal(globalCondition);

    assertThrows(IllegalArgumentException.class, () -> operators.validate());
  }

  @Test
  void testValidateThrowsWhenClientInvalid() {
    Condition clientCondition = mock(Condition.class);
    doThrow(new IllegalArgumentException("Invalid client condition")).when(clientCondition).validate();

    operators.setClient(clientCondition);

    assertThrows(IllegalArgumentException.class, () -> operators.validate());
  }
}
