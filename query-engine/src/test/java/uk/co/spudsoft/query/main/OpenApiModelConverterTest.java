/*
 * Copyright (C) 2023 njt
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

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 * @author njt
 */
public class OpenApiModelConverterTest {
  
  public OpenApiModelConverterTest() {
  }

  /**
   * Test of resolve method, of class OpenApiModelConverter.
   */
  @Test
  public void testResolve() {
    @SuppressWarnings("unchecked")
    Iterator<ModelConverter> chain = mock(Iterator.class);
    when(chain.hasNext()).thenReturn(false);

    OpenApiModelConverter oamc = new OpenApiModelConverter();
    assertNull(oamc.resolve(null, null, chain));
  }

  /**
   * Test of isOpenapi31 method, of class OpenApiModelConverter.
   */
  @Test
  public void testIsOpenapi31() {
    OpenApiModelConverter oamc = new OpenApiModelConverter();
    assertTrue(oamc.isOpenapi31());
  }

  /**
   * Test of removeEmptyProperty method, of class OpenApiModelConverter.
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void testRemoveEmptyProperty() {
    
    OpenApiModelConverter.removeEmptyProperty(null);
    
    Schema schema = mock(Schema.class);
    OpenApiModelConverter.removeEmptyProperty(schema);
    
    Map<String, Object> map = new HashMap<>();
    when(schema.getProperties()).thenReturn(map);

    OpenApiModelConverter.removeEmptyProperty(schema);
    map.put("empty", "yes");
    map.put("other", "yes");
    OpenApiModelConverter.removeEmptyProperty(schema);
    assertEquals(1, map.size());
    
  }
  
}
