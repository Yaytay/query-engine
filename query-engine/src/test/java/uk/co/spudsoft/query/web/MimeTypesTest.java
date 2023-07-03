/*
 * Copyright (C) 2023 jtalbut
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
 * @author njt
 */
public class MimeTypesTest {

  @Test
  public void testGetMimeTypeForExtension() {
    assertEquals("application/yaml", MimeTypes.getMimeTypeForExtension("yml"));
    assertNull(MimeTypes.getMimeTypeForExtension("*****"));
  }

  @Test
  public void testGetMimeTypeForFilename() {
    assertEquals("application/yaml", MimeTypes.getMimeTypeForFilename("bob.yml"));
    assertNull(MimeTypes.getMimeTypeForFilename("bob"));
  }
  
}
