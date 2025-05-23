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

import com.google.common.collect.ImmutableMap;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.tracing.TracingPolicy;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;

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
  public void testGetPersistence() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getPersistence());
    instance = new Parameters().setPersistence(null);
    assertNull(instance.getPersistence());
    Persistence audit = new Persistence().setRetryBase(Duration.ofMillis(17));
    instance = new Parameters().setPersistence(audit);
    assertEquals(17, instance.getPersistence().getRetryBase().toMillis());
  }

  @Test
  public void testIsExitOnRun() {
    Parameters instance = new Parameters();
    assertFalse(instance.isExitOnRun());
    instance = new Parameters().setExitOnRun(true);
    assertTrue(instance.isExitOnRun());
  }

  @Test
  public void testEnableAuth() {
    Parameters instance = new Parameters();
    assertNull(instance.getBasicAuth());
    instance.setBasicAuth(new BasicAuthConfig());
    assertNotNull(instance.getBasicAuth());

    assertTrue(instance.isEnableBearerAuth());
    instance.setEnableBearerAuth(false);
    assertFalse(instance.isEnableBearerAuth());
  }

  @Test
  public void testGetPipelineCache() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getPipelineCache());
    assertEquals(null, instance.getPipelineCache().getMaxDuration());
    CacheConfig cc = new CacheConfig();
    cc.setMaxDuration(Duration.ofMillis(12));
    instance.setPipelineCache(cc);
    assertEquals(12, instance.getPipelineCache().getMaxDuration().toMillis());
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
    assertEquals("query-engine", instance.getJwt().getRequiredAudiences().get(0));
    instance.getJwt().setRequiredAudiences(Collections.singletonList("aud"));
    assertEquals("[aud]", instance.getJwt().getRequiredAudiences().toString());
  }

  @Test
  public void testGetCorsAllowedOrigins() {
    Parameters instance = new Parameters();
    assertThat(instance.getCorsAllowedOrigins(), is(empty()));
    instance.setCorsAllowedOrigins(Arrays.asList("http://bob"));
    assertEquals(1, instance.getCorsAllowedOrigins().size());
    assertEquals("http://bob", instance.getCorsAllowedOrigins().get(0));
  }

  @Test
  public void testGetDefaultJwksCacheDurationSeconds() {
    Parameters instance = new Parameters();
    assertEquals(60, instance.getJwt().getDefaultJwksCacheDuration().toSeconds());
    instance.getJwt().setDefaultJwksCacheDuration(Duration.parse("PT27S"));
    assertEquals(27, instance.getJwt().getDefaultJwksCacheDuration().toSeconds());
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
    assertEquals(Arrays.asList(), instance.getJwt().getAcceptableIssuerRegexes());
    instance.getJwt().setAcceptableIssuerRegexes(Arrays.asList(".*"));
    assertEquals(Arrays.asList(".*"), instance.getJwt().getAcceptableIssuerRegexes());
  }

  @Test
  public void testGetSecrets() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getSecrets());
    assertEquals(0, instance.getSecrets().size());
    instance.setSecrets(ImmutableMap.<String, ProtectedCredentials>builder().put("first", new ProtectedCredentials("username", "password", null)).build());
    assertNotNull(instance.getSecrets());
    assertEquals("password", instance.getSecrets().get("first").getPassword());
  }

  @Test
  public void testGetSampleDataLoads() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getSampleDataLoads());
    assertEquals(0, instance.getSampleDataLoads().size());
    instance.setSampleDataLoads(
            Arrays.asList(
                    new DataSourceConfig()
            )
    );
    assertNotNull(instance.getSampleDataLoads());
    assertEquals(1, instance.getSampleDataLoads().size());
  }

  @Test
  public void testGetFileStabilisationDelaySeconds() {
    Parameters instance = new Parameters();
    assertEquals(2, instance.getFileStabilisationDelaySeconds());
    instance.setFileStabilisationDelaySeconds(23);
    assertEquals(23, instance.getFileStabilisationDelaySeconds());
  }

  @Test
  public void testGetManagementEndpointPort() {
    Parameters instance = new Parameters();
    assertNull(instance.getManagementEndpointPort());
    instance.setManagementEndpointPort(23);
    assertEquals(23, instance.getManagementEndpointPort());
  }

  @Test
  public void testGetManagementEndpoints() {
    Parameters instance = new Parameters();
    assertNotNull(instance.getManagementEndpoints());
    assertEquals(0, instance.getManagementEndpoints().size());
    instance.setManagementEndpoints(Arrays.asList("one", "two"));
    assertEquals(2, instance.getManagementEndpoints().size());
    assertEquals("one", instance.getManagementEndpoints().get(0));
    assertEquals("two", instance.getManagementEndpoints().get(1));
  }

  @Test
  public void testGetOutputCacheDir() {
    Parameters instance = new Parameters();
    assertEquals(System.getProperty("java.io.tmpdir"), instance.getOutputCacheDir());
    assertNotNull(instance.getOutputCacheDir());
    instance.setOutputCacheDir("temp");
    assertEquals("temp" + File.separator, instance.getOutputCacheDir());
    instance.setOutputCacheDir("temp\\");
    assertEquals("temp\\", instance.getOutputCacheDir());
    instance.setOutputCacheDir("temp/");
    assertEquals("temp/", instance.getOutputCacheDir());
  }

  @Test
  public void testProps() {
    String[] args = new String[]{
      "persistence.datasource.url=jdbc:bob"
      , "baseConfigPath=target/classes/samples"
      , "vertxOptions.eventLoopPoolSize=5"
      , "vertxOptions.workerPoolSize=5"
      , "httpServerOptions.tracingPolicy=ALWAYS"
    };

    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withCommandLineArgumentsGatherer(args, null)
            .create();

    Parameters p = p4j.gatherParameters();
    assertEquals("jdbc:bob", p.getPersistence().getDataSource().getUrl());
    assertEquals("target/classes/samples", p.getBaseConfigPath());
    assertEquals(5, p.getVertxOptions().getEventLoopPoolSize());
    assertEquals(5, p.getVertxOptions().getWorkerPoolSize());
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
                  	, "persistence": {
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
            .create();

    Parameters p = p4j.gatherParameters();
    assertEquals("jdbc:bob", p.getPersistence().getDataSource().getUrl());
    assertEquals("target/classes/samples", p.getBaseConfigPath());
    assertEquals(5, p.getVertxOptions().getEventLoopPoolSize());
    assertEquals(8, p.getVertxOptions().getWorkerPoolSize());
    assertEquals(TracingPolicy.ALWAYS, p.getHttpServerOptions().getTracingPolicy());

  }

  @Test
  public void testValidate() throws Exception {
    Parameters instance = new Parameters();
    instance.validate();

    String msg = assertThrows(IllegalArgumentException.class, () -> {
      Parameters config = new Parameters();
      config.getSession().getOauth().put("one", new AuthEndpoint());
      config.setJwt(null);
      config.validate();
    }).getMessage();
    assertEquals("Sessions are configured with oauth without any acceptable jwt configuration.", msg);

    msg = assertThrows(IllegalArgumentException.class, () -> {
      Parameters config = new Parameters();
      config.getSession().getOauth().put("one", new AuthEndpoint());
      config.validate();
    }).getMessage();
    assertEquals("Sessions are configured with oauth without known JWKS endpoints being configured, please set jwt.jwksEndpoints.", msg);

    msg = assertThrows(IllegalArgumentException.class, () -> {
      Parameters config = new Parameters();
      config.getSession().getOauth().put("one", new AuthEndpoint());
      config.getJwt().setJwksEndpoints(Collections.emptyList());
      config.validate();
    }).getMessage();
    assertEquals("Sessions are configured with oauth without known JWKS endpoints being configured, please set jwt.jwksEndpoints.", msg);

    instance.setLogging(null);
    instance.setTracing(null);
    instance.setSession(null);
    instance.setPipelineCache(null);
    instance.setPersistence(null);
    instance.validate();

  }

}
