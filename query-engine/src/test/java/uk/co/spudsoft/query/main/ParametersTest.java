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
package uk.co.spudsoft.query.main;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.tracing.zipkin.ZipkinTracingOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.query.json.TracingOptionsMixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class ParametersTest {
  
  @Test
  public void testGetVertxOptions() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getVertxOptions());
    instance = new Parameters().setVertxOptions(null);
    assertNull(instance.getVertxOptions());
    VertxOptions vo = new VertxOptions()
            .setHAGroup("haGroup")
            ;
    instance = new Parameters().setVertxOptions(vo);
    assertEquals(vo.toJson(), instance.getVertxOptions().toJson());
    assertEquals("haGroup", instance.getVertxOptions().getHAGroup());
  }

  @Test
  public void testGetHttpServerOptions() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getHttpServerOptions());
    instance = new Parameters().setHttpServerOptions(null);
    assertNull(instance.getHttpServerOptions());
    HttpServerOptions hso = new HttpServerOptions()
            .setHost("host")
            ;
    instance = new Parameters().setHttpServerOptions(hso);
    assertEquals(hso.toJson(), instance.getHttpServerOptions().toJson());
    assertEquals("host", instance.getHttpServerOptions().getHost());
  }

  @Test
  public void testGetAudit() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getAudit());
    instance = new Parameters().setAudit(null);
    assertNull(instance.getAudit());
    Audit audit = new Audit().setRetryBaseMs(17);
    instance = new Parameters().setAudit(audit);
    assertEquals(17, instance.getAudit().getRetryBaseMs());
  }
  
  @Test
  public void testIsExitOnRun() {
    Parameters instance = new Parameters();
    assertFalse(instance.isExitOnRun());
    instance = new Parameters().setExitOnRun(true);
    assertTrue(instance.isExitOnRun());
  }

  @Test
  public void testGetBaseConfigPath() {
    Parameters instance = new Parameters();
    assertEquals("/var/query-engine", instance.getBaseConfigPath());
    instance = new Parameters().setBaseConfigPath("baseConfigPath");
    assertEquals("baseConfigPath", instance.getBaseConfigPath());
  }
  
  @Test
  public void testGetAudience() {
    Parameters instance = new Parameters();
    assertEquals("query-engine", instance.getAudience());
    instance = new Parameters().setBaseConfigPath("aud");
    assertEquals("aud", instance.getBaseConfigPath());
  }
  
  @Test
  public void testGetDefaultJwksCacheDurationSeconds() {
    Parameters instance = new Parameters();
    assertEquals(60, instance.getDefaultJwksCacheDurationSeconds());
    instance = new Parameters().setDefaultJwksCacheDurationSeconds(27);
    assertEquals(27, instance.getDefaultJwksCacheDurationSeconds());
  }
  
  @Test
  public void testGetOpenIdIntrospectionHeaderName() {
    Parameters instance = new Parameters();
    assertEquals(null, instance.getOpenIdIntrospectionHeaderName());
    instance = new Parameters().setOpenIdIntrospectionHeaderName("OpenIdIntrospectionHeaderName");
    assertEquals("OpenIdIntrospectionHeaderName", instance.getOpenIdIntrospectionHeaderName());
  }
  
  @Test
  public void testGetAcceptableIssuerRegexes() {
    Parameters instance = new Parameters();
    assertEquals(null, instance.getAcceptableIssuerRegexes());
    instance = new Parameters().setAcceptableIssuerRegexes(Arrays.asList(".*"));
    assertEquals(Arrays.asList(".*"), instance.getAcceptableIssuerRegexes());
  }
  
  @Test
  public void testProps() {
    String[] args = new String[]{
      "audit.datasource.url=jdbc:bob"
      , "baseConfigPath=target/classes/samples"
      , "vertxOptions.eventLoopPoolSize=5"
      , "vertxOptions.workerPoolSize=5"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "httpServerOptions.tracingPolicy=ALWAYS"
    };
    
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withCommandLineArgumentsGatherer(args, null)
            .withMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .create();

    Parameters p = p4j.gatherParameters();
    assertEquals("jdbc:bob", p.getAudit().getDataSource().getUrl());
    assertEquals("target/classes/samples", p.getBaseConfigPath());
    assertEquals(5, p.getVertxOptions().getEventLoopPoolSize());
    assertEquals(5, p.getVertxOptions().getWorkerPoolSize());
    assertEquals("Query-Engine", ((ZipkinTracingOptions) p.getVertxOptions().getTracingOptions()).getServiceName());
    assertEquals(TracingPolicy.ALWAYS, p.getHttpServerOptions().getTracingPolicy());
    
  }

  @Test
  public void testJson() throws Exception {
    File outputDir = new  File("target/parameters-test");
    outputDir.mkdirs();
    File jsonFile = new File(outputDir, "config.json");
    
    String json = """
                  {
                      "vertxOptions": {
                          "eventLoopPoolSize": 5
                          , "workerPoolSize": 8
                          , "tracingOptions": {
                              "serviceName":"Query-Engine"
                          }
                      }
                  	, "httpServerOptions": {
                          "tracingPolicy": "ALWAYS"
                      }
                  	, "baseConfigPath": "target/classes/samples"
                  	, "audit": {
                          "dataSource": {
                              "url": "jdbc:bob"
                          }
                      }
                  }
                  """;
    try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
      fos.write(json.getBytes(StandardCharsets.UTF_8));
    }
    
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(outputDir, FileType.Json)
            .withMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .create();

    Parameters p = p4j.gatherParameters();
    assertEquals("jdbc:bob", p.getAudit().getDataSource().getUrl());
    assertEquals("target/classes/samples", p.getBaseConfigPath());
    assertEquals(5, p.getVertxOptions().getEventLoopPoolSize());
    assertEquals(8, p.getVertxOptions().getWorkerPoolSize());
    assertEquals("Query-Engine", ((ZipkinTracingOptions) p.getVertxOptions().getTracingOptions()).getServiceName());
    assertEquals(TracingPolicy.ALWAYS, p.getHttpServerOptions().getTracingPolicy());
    
  }

}
