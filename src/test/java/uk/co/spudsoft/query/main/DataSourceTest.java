/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 * @author jtalbut
 */
public class DataSourceTest {
  
  @Test
  public void testGetUrl() {
    DataSource ds = DataSource.builder().url("url").build();
    assertThat(ds.getUrl(), equalTo("url"));
  }

  @Test
  public void testGetSchema() {
    DataSource ds = DataSource.builder().schema("schema").build();
    assertThat(ds.getSchema(), equalTo("schema"));
  }

  @Test
  public void testGetUser() {
    DataSource ds = DataSource.builder().user(Credentials.builder().username("user").build()).build();
    assertThat(ds.getUser().getUsername(), equalTo("user"));
  }

  @Test
  public void testGetAdminUser() {
    DataSource ds = DataSource.builder().adminUser(Credentials.builder().username("admin").build()).build();
    assertThat(ds.getAdminUser().getUsername(), equalTo("admin"));
  }

  @Test
  public void testGetMaxPoolSize() {
    DataSource ds = DataSource.builder().build();
    assertThat(ds.getMaxPoolSize(), equalTo(10));    
    ds = DataSource.builder().maxPoolSize(17).build();
    assertThat(ds.getMaxPoolSize(), equalTo(17));
  }

}
