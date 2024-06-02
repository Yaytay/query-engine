/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ImmutableCollectionToolsTest {
  

  /**
   * Test of copy method, of class ImmutableCollectionTools.
   */
  @Test
  public void testCopy_Map() {
    Map<String, String> src = null;
    Map<String, String> dst = ImmutableCollectionTools.copy(src);
    assertNotNull(dst);
    assertTrue(dst.isEmpty());
  }

  /**
   * Test of copy method, of class ImmutableCollectionTools.
   */
  @Test
  public void testCopy_List() {
    List<String> src = null;
    List<String> dst = ImmutableCollectionTools.copy(src);
    assertNotNull(dst);
    assertTrue(dst.isEmpty());
  }
  
}
