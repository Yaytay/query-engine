/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class EndpointTest {
  
  @Test
  public void testGetType() {
    Endpoint instance = Endpoint.builder().type(EndpointType.SQL).build();
    assertEquals(EndpointType.SQL, instance.getType());
  }

  @Test
  public void testGetUrl() {
    Endpoint instance = Endpoint.builder().url("url").build();
    assertEquals("url", instance.getUrl());
  }

  @Test
  public void testGetUsername() {
    Endpoint instance = Endpoint.builder().username("username").build();
    assertEquals("username", instance.getUsername());
  }

  @Test
  public void testGetPassword() {
    Endpoint instance = Endpoint.builder().password("password").build();
    assertEquals("password", instance.getPassword());
  }
  
}
