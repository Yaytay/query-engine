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
package uk.co.spudsoft.query.exec;

import com.google.common.net.MediaType;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;


/**
 *
 * @author jtalbut
 */
public class FormatRequestTest {
  
  @Test
  public void testGetAccept() {
    FormatRequest dr = FormatRequest.builder().build();
    assertThat(dr.getAccept(), hasSize(0));
    dr = FormatRequest.builder().accept("").build();
    assertThat(dr.getAccept(), hasSize(0));
    dr = FormatRequest.builder().accept(Arrays.asList()).build();
    assertNotNull(dr.getAccept());
    assertThat(dr.getAccept(), hasSize(0));
    dr = FormatRequest.builder().accept(Arrays.asList(MediaType.BZIP2)).build();
    assertNotNull(dr.getAccept());
    assertThat(dr.getAccept(), hasSize(1));
    assertEquals("application/x-bzip2", dr.getAccept().get(0).toString());
  }
  
  @Test
  public void testRanking() {
    assertEquals(0, FormatRequest.compareMediaTypePriorities(MediaType.ANY_TYPE, MediaType.ANY_TYPE));
    assertEquals(1, FormatRequest.compareMediaTypePriorities(MediaType.ANY_TYPE, MediaType.ANY_APPLICATION_TYPE));
    assertEquals(-1, FormatRequest.compareMediaTypePriorities(MediaType.ANY_APPLICATION_TYPE, MediaType.ANY_TYPE));
    assertEquals(-1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY, MediaType.ANY_APPLICATION_TYPE));
    assertEquals(1, FormatRequest.compareMediaTypePriorities(MediaType.ANY_APPLICATION_TYPE, MediaType.APPLICATION_BINARY));
    assertEquals(-1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY.withParameter("q", "1.0"), MediaType.BMP.withParameter("q", "0.1")));
    assertEquals(1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY.withParameter("q", "0.1"), MediaType.BMP.withParameter("q", "1.0")));
    assertEquals(-1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY.withParameter("q", "1.0"), MediaType.BMP.withParameter("q", "BAD")));
    assertEquals(1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY.withParameter("q", "BAD"), MediaType.BMP.withParameter("q", "1.0")));
    assertEquals(-1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY, MediaType.BMP.withParameter("q", "0.1")));
    assertEquals(1, FormatRequest.compareMediaTypePriorities(MediaType.APPLICATION_BINARY.withParameter("q", "0.1"), MediaType.BMP));
  }

}
