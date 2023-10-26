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

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class ConcurrentRuleIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ConcurrentRuleIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/samples-mainit");
    paramsDir.mkdirs();
  }
      
  @Test
  public void testMainDaemon() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--audit.datasource.url=jdbc:" + postgres.getUrl()
      , "--audit.datasource.adminUser.username=" + postgres.getUser()
      , "--audit.datasource.adminUser.password=" + postgres.getPassword()
      , "--audit.datasource.schema=public" 
      , "--baseConfigPath=target/query-engine/samples-concurrentrulesit"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=false"
//      , "--logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec=TRACE"
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=8001"
      , "--managementEndpointUrl=http://localhost:8001/manage"
    }, stdout);
    assertEquals(0, stdoutStream.size());
    
    RestAssured.port = main.getPort();
    
    long startTime = System.currentTimeMillis();
    given()
            .log().all()
            .get("/query/sub1/sub2/ConcurrentRulesIT?_fmt=oneline")
            .then()
            .statusCode(200)
            .log().all()
            ;
    long endTime = System.currentTimeMillis();
    assertThat(endTime - startTime, greaterThan(5000L));
    
    List<Response> responses = new ArrayList<>();
    Thread thread1 = new Thread(() -> runQuery(responses));
    thread1.start();
    Thread.sleep(100);
    Thread thread2 = new Thread(() -> runQuery(responses));
    thread2.start();
    
    thread1.join();
    thread2.join();
    
    if (responses.get(0).getStatusCode() == 200) {
      assertEquals(200, responses.get(0).getStatusCode());
      assertEquals(429, responses.get(1).getStatusCode());
    } else {
      assertEquals(429, responses.get(0).getStatusCode());
      assertEquals(200, responses.get(1).getStatusCode());
    }
    
    main.shutdown();
  }
  
  private void runQuery(List<Response> responses) {
    Response response = given()
            .log().all()
            .get("/query/sub1/sub2/ConcurrentRulesIT?_fmt=oneline")
            .then()
            .log().all()
            .extract().response()
            ;
    synchronized(responses) {
      responses.add(response);
    }
  }
  
}
