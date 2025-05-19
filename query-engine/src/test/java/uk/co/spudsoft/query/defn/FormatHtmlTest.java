/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.defn;

import com.google.common.net.MediaType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class FormatHtmlTest {
  
  @Test
  public void testBuilder() {
    FormatHtml dh = FormatHtml.builder().build();
    assertEquals(FormatType.HTML, dh.getType());
    try {
      FormatHtml.builder().type(FormatType.JSON).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
    assertEquals("html", dh.getExtension());
    assertEquals("html", dh.getName());
    assertEquals(MediaType.parse("text/html"), dh.getMediaType());
    dh = FormatHtml.builder().extension("extn").build();
    assertEquals("extn", dh.getExtension());
    dh = FormatHtml.builder().name("format").build();
    assertEquals("format", dh.getName());
    dh = FormatHtml.builder().mediaType("image/gif").build();
    assertEquals(MediaType.GIF, dh.getMediaType());    
    dh = FormatHtml.builder().description("desc").build();
    assertEquals("desc", dh.getDescription());
    assertFalse(dh.isHidden());
    dh = FormatHtml.builder().hidden(true).build();
    assertTrue(dh.isHidden());
    dh = FormatHtml.builder().filename("file").build();
    assertEquals("file", dh.getFilename());
  }
  
  @Test
  public void testValidate() {
    FormatHtml.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatHtml.builder().name(null).build().validate();
    });
    FormatHtml.builder().name("name").build().validate();
  }
}
