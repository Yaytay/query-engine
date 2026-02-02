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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.exec.context.RequestContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatorsInstanceTest {

  private RequestContext requestContext;

  @BeforeEach
  void setUp() {
    requestContext = mock(RequestContext.class);
  }

  @Test
  @DisplayName("Constructor handles null configuration")
  void testConstructorWithNullConfig() {
    OperatorsInstance instance = new OperatorsInstance(null);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertFalse(flags.global());
    assertFalse(flags.client());
    assertEquals("normal user", flags.toString());
  }

  @Test
  @DisplayName("Evaluate returns false when configuration contains no conditions")
  void testEvaluateWithEmptyConfig() {
    Operators operators = new Operators();
    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertFalse(flags.global());
    assertFalse(flags.client());
  }

  @Test
  @DisplayName("Correctly identifies a global operator using real JEXL")
  void testEvaluateGlobalOperator() {
    Operators operators = new Operators();
    // Condition: true if host is 'admin.local'
    operators.setGlobal(new Condition("request.host == 'admin.local'"));

    when(requestContext.getHost()).thenReturn("admin.local");

    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertTrue(flags.global());
    assertFalse(flags.client());
    assertEquals("global operator", flags.toString());
  }

  @Test
  @DisplayName("Correctly identifies a client operator using real JEXL")
  void testEvaluateClientOperator() {
    Operators operators = new Operators();
    // Condition: true if there is a 'client-op' header
    operators.setClient(new Condition("request.headers.contains('client-op')"));

    io.vertx.core.MultiMap headers = io.vertx.core.MultiMap.caseInsensitiveMultiMap();
    headers.add("client-op", "true");
    when(requestContext.getHeaders()).thenReturn(headers);

    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertFalse(flags.global());
    assertTrue(flags.client());
    assertEquals("client operator", flags.toString());
  }

  @Test
  @DisplayName("Handles both flags and verifies toString precedence")
  void testEvaluateBothOperators() {
    Operators operators = new Operators();
    operators.setGlobal(new Condition("true"));
    operators.setClient(new Condition("true"));

    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertTrue(flags.global());
    assertTrue(flags.client());
    // Precedence: global > client > normal
    assertEquals("global operator", flags.toString());
  }

  @Test
  @DisplayName("Returns normal user when JEXL conditions evaluate to false")
  void testEvaluateConditionsFail() {
    Operators operators = new Operators();
    operators.setGlobal(new Condition("false"));
    operators.setClient(new Condition("1 == 2"));

    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertFalse(flags.global());
    assertFalse(flags.client());
    assertEquals("normal user", flags.toString());
  }

  @Test
  @DisplayName("Verify mixed null and real conditions")
  void testMixedConditions() {
    Operators operators = new Operators();
    operators.setClient(new Condition("true"));
    // Global is left as null

    OperatorsInstance instance = new OperatorsInstance(operators);
    OperatorsInstance.Flags flags = instance.evaluate(requestContext);

    assertFalse(flags.global());
    assertTrue(flags.client());
    assertEquals("client operator", flags.toString());
  }

  @Test
  @DisplayName("Flags toString for normal user")
  void testFlagsToStringNormal() {
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(false, false);
    assertEquals("normal user", flags.toString());
  }
}
