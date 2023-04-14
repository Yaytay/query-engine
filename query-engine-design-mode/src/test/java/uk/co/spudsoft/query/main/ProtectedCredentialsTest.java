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
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import uk.co.spudsoft.query.defn.Condition;

/**
 *
 * @author jtalbut
 */
public class ProtectedCredentialsTest {
  
  @Test
  public void testGetCondition() {
    ProtectedCredentials pc = new ProtectedCredentials(null, null, null);
    assertNull(pc.getCondition());
    pc.setCondition(new Condition("true"));
    assertEquals("true", pc.getCondition().getExpression());
  }

  @Test
  public void testToString() {
    ProtectedCredentials pc = new ProtectedCredentials();
    pc.setCondition(new Condition("true"));
    assertEquals("{\"condition\":\"true\"}", pc.toString());
    
    pc.setUsername("user");
    assertEquals("{\"username\":\"user\", \"condition\":\"true\"}", pc.toString());
    
    pc.setPassword("pass");
    assertEquals("{\"username\":\"user\", \"condition\":\"true\"}", pc.toString());
  }
  
}
