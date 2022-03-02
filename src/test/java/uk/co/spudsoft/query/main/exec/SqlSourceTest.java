/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class SqlSourceTest {
  
  @Test
  public void testGetUrl() {
    SqlSource instance = new SqlSource("url", "username", "password");
    assertEquals("url", instance.getUrl());
  }

  @Test
  public void testGetUsername() {
    SqlSource instance = new SqlSource("url", "username", "password");
    assertEquals("username", instance.getUsername());
  }

  @Test
  public void testGetPassword() {
    SqlSource instance = new SqlSource("url", "username", "password");
    assertEquals("password", instance.getPassword());
  }
  
}
