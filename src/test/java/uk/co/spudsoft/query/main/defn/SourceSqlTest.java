/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class SourceSqlTest {
  
  @Test
  public void testGetType() {
    SourceSql instance = SourceSql.builder().build();
    assertEquals(SourceType.SQL, instance.getType());
  }

  @Test
  public void testGetEndpoint() {
    SourceSql instance = SourceSql.builder().endpoint("endpoint").build();
    assertEquals("endpoint", instance.getEndpoint());
  }

  @Test
  public void testGetQuery() {
    SourceSql instance = SourceSql.builder().query("query").build();
    assertEquals("query", instance.getQuery());
  }

  @Test
  public void testGetPoolOptions() {
    PoolOptions options = new PoolOptions().setConnectionTimeout(1234);
    SourceSql instance = SourceSql.builder().poolOptions(options).build();
    assertEquals(1234, instance.getPoolOptions().getConnectionTimeout());
  }

  @Test
  public void testGetStreamingFetchSize() {
    SourceSql instance = SourceSql.builder().streamingFetchSize(2345).build();
    assertEquals(2345, instance.getStreamingFetchSize());
  }
  
}
