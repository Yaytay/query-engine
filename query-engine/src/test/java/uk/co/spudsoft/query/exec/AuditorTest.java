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

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.main.Persistence;
import uk.co.spudsoft.query.main.DataSourceConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class AuditorTest {

  private static final Logger logger = LoggerFactory.getLogger(AuditorTest.class);

  @Test
  public void testBadDriver() {
    String url = "jdbc:nonexistant:wibble";
    logger.info("Bad driver: {}", url);
    AuditorImpl auditor = new AuditorImpl(
            null,
            null,
            new Persistence()
                    .setDataSource(new DataSourceConfig()
                                    .setUrl(url)
                    )
                    .setRetryBaseMs(10000)
                    .setRetryIncrementMs(10000)
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch (Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, lessThan(1000L));
    }
  }

  @Test
  public void testNoDatasource() {
    String url = null;
    logger.info("No datasource: {}", url);
    AuditorImpl auditor = new AuditorImpl(
            null,
            null,
            new Persistence()
                    .setDataSource(null)
                    .setRetryBaseMs(100)
                    .setRetryIncrementMs(10)
                    .setRetryLimit(10)
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch (Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, lessThan(100L));
    }
  }

  @Test
  public void testBadUrl() {
    String url = "jdbc:postgresql://wibble/db";
    logger.info("Bad URL: {}", url);
    AuditorImpl auditor = new AuditorImpl(
            null,
            null,
            new Persistence()
                    .setDataSource(new DataSourceConfig()
                                    .setUrl(url)
                    )
                    .setRetryBaseMs(100)
                    .setRetryIncrementMs(100)
                    .setRetryLimit(4)
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch (Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
    }
  }

  @Test
  public void testNoRetries() {
    String url = "jdbc:postgresql://wibble/db";
    logger.info("Bad URL: {}", url);
    AuditorImpl auditor = new AuditorImpl(
            null,
            null,
            new Persistence()
                    .setDataSource(new DataSourceConfig()
                                    .setUrl(url)
                    )
                    .setRetryLimit(0)
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch (Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
    }
  }
  
  @Test
  public void testLocalUser() {
    assertNull(AuditorImpl.localizeUsername(null));
    assertEquals("Bob", AuditorImpl.localizeUsername("Bob"));
    assertEquals("Bob", AuditorImpl.localizeUsername("Bob@somewhere"));
  }
  
  @Test
  public void testListToJson() {
    assertNull(AuditorImpl.listToJson(null));
    JsonArray ja = AuditorImpl.listToJson(Arrays.asList("one", "two"));
    assertEquals(2, ja.size());
    assertEquals("one", ja.getValue(0));
    assertEquals("two", ja.getValue(1));
  }
  
  @Test
  public void testMultimapToJson() {
    assertNull(AuditorImpl.multiMapToJson(null));
    MultiMap map= new HeadersMultiMap()
            .add("one", "first")
            .add("two", "second")
            .add("two", "third")
            .add(HttpHeaders.AUTHORIZATION.toString(), "Bearer a.b.c")
            ;
    JsonObject jo = AuditorImpl.multiMapToJson(map);
    assertEquals(3, jo.size());
    assertEquals("first", jo.getValue("one"));
    assertEquals(new JsonArray().add("second").add("third"), jo.getValue("two"));
    assertEquals("Bearer a.b", jo.getValue(HttpHeaders.AUTHORIZATION.toString()));
  }
  
  @Test
  public void testProtectAuth() {
    Base64.Encoder encoder = Base64.getUrlEncoder();
    
    assertEquals("bob", AuditorImpl.protectAuthHeader("bob"));
    assertEquals("Basic YQ==", AuditorImpl.protectAuthHeader("Basic " + new String(encoder.encode("a:b".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)));
    assertEquals("Basic YQ==", AuditorImpl.protectAuthHeader("Basic " + new String(encoder.encode("a".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)));
    assertEquals("Basic ", AuditorImpl.protectAuthHeader("Basic "));
    assertEquals("Bearer a.b", AuditorImpl.protectAuthHeader("Bearer a.b.c"));
  }
  
}
