/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/**
 *
 * @author jtalbut
 */
public class SqlQueryTest {
  
  @Test
  public void testGetSource() {
    SqlQuery query = new SqlQuery(null, "query");
    assertNull(query.getSource());
    SqlSource source = new SqlSource("url", "username", "password");
    query = new SqlQuery(source, "query");
    assertSame(source, query.getSource());
  }

  @Test
  public void testGetQuery() {
    SqlQuery query = new SqlQuery(null, "query");
    assertEquals("query", query.getQuery());
  }
  
}
