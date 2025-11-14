/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.TokenBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkJwksHandler;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;
import uk.co.spudsoft.query.main.Main;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;


/**
 *
 * @author jtalbut
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginRouterWithForceJwtIT {
  
  private static final Logger logger = LoggerFactory.getLogger(LoginRouterWithForceJwtIT.class);
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  private JdkJwksHandler jwks;
  private TokenBuilder tokenBuilder;
  
  @BeforeAll
  public void createDirs() throws IOException {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
    
    Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));
    tokenBuilder = new JdkTokenBuilder(keyCache);

    jwks = JdkJwksHandler.create();
    logger.debug("Starting JWKS endpoint");
    jwks.start();
    jwks.setKeyCache(keyCache);
  }
  
  @Test
  public void testLoginWithoutDiscoveryIT() throws Exception {
    logger.debug("Running testLoginWithoutDiscoveryIT");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=" + CONFS_DIR
      , "--jwt.acceptableIssuerRegexes[0]=" + jwks.getBaseUrl()
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--jwt.jwksEndpoints[0]=" + jwks.getBaseUrl() + "/jwks"
      , "--logging.jsonFormat=false"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=8001"
      , "--managementEndpointUrl=http://localhost:8001/manage"
      , "--session.requireSession=false"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
      , "--enableForceJwt=true"
    }, stdout, System.getenv());
    assertEquals(0, stdoutStream.size());
    
    String token = tokenBuilder.buildToken(JsonWebAlgorithm.RS512
            , "LoginRouterWithForceJwtIT-" + UUID.randomUUID().toString()
            , jwks.getBaseUrl()
            , "sub" + System.currentTimeMillis()
            , Arrays.asList("query-engine")
            , System.currentTimeMillis() / 1000
            , System.currentTimeMillis() / 1000 + 3600
            , ImmutableMap.<String, Object>builder()
                    .put("name", "Full Name")
                    .put("preferred_username", "username")
                    .build()
    );
    
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .body(token)
            .post("/login/forcejwt")
            .then()
            .statusCode(405)
            .log().all()
            .cookies(Collections.emptyMap())
            .extract().body().asString()
            ;
    assertEquals("", body);
    
    body = given()
            .log().all()
            .body(token)
            .put("/login/forcejwt")
            .then()
            .statusCode(200)
            .log().all()
            .cookie("qe-session", allOf(notNullValue(), hasLength(greaterThan(99))))
            .extract().body().asString()
            ;
    assertEquals("Session started", body);
    
  }
  
}
