/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author jtalbut
 */
public class SqlQueryProcessorTest {
  
  @Test
  public void testConstructor() {
    SqlQueryProcessor processor = new SqlQueryProcessor();
    assertNotNull(processor);
  }
  
}
