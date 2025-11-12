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
package uk.co.spudsoft.query.exec.procs.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class RsqlComparatorStringTest {
  
  @Test
  public void testEqual() {
    
    RsqlComparatorString comp = new RsqlComparatorString();
    
    assertFalse(comp.equal(null, "comp"));
    assertTrue(comp.equal(null, null));
    assertFalse(comp.equal("row", null));
    
    assertFalse(comp.equal("row", "comp"));
    assertTrue(comp.equal("value", "value"));

    assertTrue(comp.equal("value", "*lue"));
    assertFalse(comp.equal("values", "*lue"));

    assertTrue(comp.equal("value", "*lu*"));
    assertFalse(comp.equal("values", "*lud*"));

    assertTrue(comp.equal("value", "val*"));
    assertTrue(comp.equal("validation", "val*"));
    assertFalse(comp.equal("bigvalidation", "val*"));

  }
  
}
