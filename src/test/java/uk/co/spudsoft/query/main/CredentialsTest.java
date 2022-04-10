/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 *
 * @author jtalbut
 */
public class CredentialsTest {
  
  @Test
  public void testToString() {
    Credentials creds = Credentials.builder().username("user").password("pass").build();
    assertThat(creds.toString(), not(containsString("pass")));
    creds = Credentials.builder().password("pass").build();
    assertThat(creds.toString(), not(containsString("pass")));
  }

  @Test
  public void testGetUsername() {
    Credentials creds = Credentials.builder().username("user").password("pass").build();
    assertThat(creds.getUsername(), equalTo("user"));
  }

  @Test
  public void testGetPassword() {
    Credentials creds = Credentials.builder().username("user").password("pass").build();
    assertThat(creds.getPassword(), equalTo("pass"));
  }

}
