/*
 * Copyright (C) 2024 jtalbut
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class HistoryHandlerTest {
  
  @Test
  public void testBoundInt() {
    assertEquals(3, HistoryHandler.boundInt(3, 0, 0, 10));
    assertEquals(3, HistoryHandler.boundInt(null, 3, 0, 10));
    assertEquals(3, HistoryHandler.boundInt(-1, 7, 3, 10));
    assertEquals(10, HistoryHandler.boundInt(13, 7, 3, 10));
  }
  
}
