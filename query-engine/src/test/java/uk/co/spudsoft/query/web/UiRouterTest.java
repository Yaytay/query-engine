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
package uk.co.spudsoft.query.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class UiRouterTest {
  
  @Test
  public void testRemoveDots() {    
    assertEquals("/", UiRouter.removeDots("/"));
    assertEquals("/one/two/three", UiRouter.removeDots("/one/two/three"));
    assertEquals("/one/two/three", UiRouter.removeDots("/one/two/./three"));
    assertEquals("/one/two/three", UiRouter.removeDots("/one/two//three"));
    assertEquals("/one/three", UiRouter.removeDots("/one/two/../three"));
    assertEquals("/three", UiRouter.removeDots("/one/two/../../three"));
    assertEquals("/three", UiRouter.removeDots("/one/two/../../../three"));
    assertEquals("/three", UiRouter.removeDots("/../../../three"));
  }
  
  @Test
  public void testGetFileExtension() {    
    assertEquals("txt", UiRouter.getFileExtension("one.txt"));
    assertEquals("txt", UiRouter.getFileExtension("one/two.txt"));
    assertNull(UiRouter.getFileExtension("one"));
    assertNull(UiRouter.getFileExtension("one."));
    assertEquals("txt", UiRouter.getFileExtension(".txt"));
  }
  
}
