/*
 * Copyright (C) 2025 njt
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

import io.netty.handler.codec.http.QueryStringDecoder;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.web.MockOidcServer.LogoutRequest;

/**
 * Abstract class for performing login/logout tests.
 * 
 * The options are:
 * * Discovery vs Config
 * * Database vs Memory
 * * BackLogout vs FrontLogout vs NoLogout
 * Not all permutations are valid, FrontLogout and NoLogout will only be tested with Config and not all permutations are required.
 * 
 * @author njt
 */
public abstract class AbstractLoginLogoutTester {

  private static final Logger logger = LoggerFactory.getLogger(AbstractLoginLogoutTester.class);

  protected MockOidcServer oidc;
  protected int oidcPort;

  protected void startOidc() throws Exception {
    oidc = MockOidcServer.builder()
            .supportsFrontChannelLogout(true)
            .supportsBackChannelLogout(true)
            .build();
    oidc.start();
    oidcPort = oidc.getActualPort();
  }
  
  protected void stopOidc() {
    oidc.stop();
  }

  protected void performLogin(int qePort, String scope, Map<String, String> cookies) {
    // OAuth dance
    // 1. Call /login endpoint with provider and return path.
    //    Should redirect to login provider auth page.
    // 2. Sign in to login provider.
    //    Should redirect to /login/return with auth code
    // 3. Call /login/return with correct params.
    //    Should redirect to original return path with session cookie.

    // 1. Call /login endpoint with provider and return path.
    //    Should redirect to login provider auth page.
    String targetUrl = "/";
    String authUiUrl = given()
            .redirects().follow(false)
            .cookies(cookies)
            .log().all()
            .get("/login?provider=test&return=" + targetUrl)
            .then()
            .statusCode(307)
            .log().all()
            .extract().header("Location");
    assertThat(authUiUrl, startsWith("http://localhost:" + oidcPort + "/auth?"));
    logger.debug("authUiUrl: {}", authUiUrl);
    Map<String, List<String>> authUiParams = new QueryStringDecoder(authUiUrl).parameters();
    assertEquals(Arrays.asList("code"), authUiParams.get("response_type"));
    assertEquals(Arrays.asList("test-client"), authUiParams.get("client_id"));
    assertEquals(Arrays.asList("S256"), authUiParams.get("code_challenge_method"));
    assertTrue(authUiParams.containsKey("state"));
    assertTrue(authUiParams.containsKey("code_challenge"));
    assertTrue(authUiParams.containsKey("nonce"));
    assertThat(authUiParams.get("redirect_uri").get(0), startsWith("http://localhost:" + qePort + "/login/return"));

    // 2. Sign in to login provider.
    //    Should redirect to /login/return with auth code
    String returnUrl = given()
            .redirects().follow(false)
            .log().all()
            .contentType("application/x-www-form-urlencoded")
            .formParam("username", "test-user")
            .formParam("password", "test-password")
            .post(authUiUrl)
            .then()
            .log().all()
            .statusCode(302)
            .extract().header("Location");

    // This Location should now be your app’s redirect_uri with code and state appended
    assertThat(returnUrl, startsWith("http://localhost:" + qePort + "/login/return"));
    Map<String, List<String>> returnUrlParams = new QueryStringDecoder(returnUrl).parameters();
    assertTrue(returnUrlParams.containsKey("state"));
    assertTrue(returnUrlParams.containsKey("code"));

    // 3. Call /login/return with correct params.
    //    Should redirect to original return path with session cookie.
    Map<String, String> newCookies = given()
            .redirects().follow(false)
            .log().all()
            .get(returnUrl)
            .then()
            .log().all()
            .statusCode(307)
            .header("Location", equalTo("http://localhost:" + qePort + targetUrl))
            .extract().cookies();
    logger.debug("Cookies: {}", newCookies);
    assertTrue(newCookies.containsKey("qe-session"));
    assertThat(newCookies.get("qe-session").length(), greaterThan(64));
    cookies.putAll(newCookies);
  }

  void performStandardLoggedInOperations(Map<String, String> cookies, String dbName, int dbPort) {
    String body;
    given()
            .queryParam("key", dbName)
            .queryParam("port", dbPort)
            .cookies(cookies)
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200);
    body = given()
            .cookies(cookies)
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract()
            .body().asString();
    logger.info("History: {}", body);
    body = given()
            .cookies(cookies)
            .queryParam("key", dbName)
            .queryParam("port", dbPort)
            .queryParam("_runid", UUID.randomUUID().toString())
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract()
            .body().asString();
    assertThat(body, startsWith("[\n{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},\n{\"dataId\":2,\"instant\":\"1971-05-08T06:00\",\"ref\":\"aqua\",\"value\":\"second\",\"children\":\"two,four\",\"DateField\":\"2023-05-04\",\"TimeField\":\"23:58\",\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},"));
    body = given()
            .cookies(cookies)
            .log()
            .all()
            .get("/api/history")
            .then()
            .log()
            .ifError()
            .statusCode(200)
            .extract()
            .body().asString();
    logger.debug("History: {}", body);
  }

  /**
   * The web request for front channel and back channel logout is the same, but the results are different.
   * @param cookies 
   */
  void performBackChannelLogout(Map<String, String> cookies) {
    
    assertThat(oidc.getLogoutRequests(), is(empty()));
    
    String body;
    cookies = given()
            .cookies(cookies)
            .redirects().follow(false)
            .log().all()
            .get("/logout")
            .then()
            .log().all()
            .statusCode(307).header("Location", equalTo("/"))
            .extract()
            .cookies();
    logger.debug("Cookies: {}", cookies);
    assertTrue(cookies.containsKey("qe-session"));
    assertThat(cookies.get("qe-session").length(), equalTo(0));
    
    List<LogoutRequest> logoutRequests = oidc.getLogoutRequests();
    assertThat(logoutRequests, hasSize(1));
    assertFalse(logoutRequests.get(0).frontChannel());
    assertNull(logoutRequests.get(0).idToken());
    assertNotNull(logoutRequests.get(0).accessToken());
    assertNull(logoutRequests.get(0).refreshToken());
    
    cookies = new HashMap<>();
    cookies.put("qe-session", "bad");
    body = given()
            .cookies(cookies)
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract()
            .body().asString();
    logger.debug("History: {}", body);
  }

  /**
   * The web request for front channel and back channel logout is the same, but the results are different.
   * @param cookies 
   */
  void performFrontChannelLogout(int qePort, Map<String, String> cookies) {
    
    assertThat(oidc.getLogoutRequests(), is(empty()));
    
    String body;
    Response response = given()
            .cookies(cookies)
            .redirects().follow(false)
            .log().all()
            .get("/logout")
            .then()
            .log().all()
            .header("Location", startsWith(oidc.getLogoutUrl()))
            .header("Location", containsString("?id_token_hint"))
            .header("Location", containsString("&client_id"))
            .extract()
            .response();
    cookies = response.cookies();
    String location = response.header("Location");
    logger.debug("Cookies: {}", cookies);
    assertTrue(cookies.containsKey("qe-session"));
    assertThat(cookies.get("qe-session").length(), equalTo(0));
    
    assertThat(oidc.getLogoutRequests(), is(empty()));

    cookies = given()
            .cookies(cookies)
            .redirects().follow(false)
            .log().all()
            .get(location)
            .then()
            .log().all()
            .statusCode(302)
            .header("Location", equalTo("http://localhost:" + qePort + "/"))
            .extract()
            .cookies();
    
    List<LogoutRequest> logoutRequests = oidc.getLogoutRequests();
    assertThat(logoutRequests, hasSize(1));
    assertTrue(logoutRequests.get(0).frontChannel());
    assertNotNull(logoutRequests.get(0).idToken());
    assertNull(logoutRequests.get(0).accessToken());
    assertNull(logoutRequests.get(0).refreshToken());
    
    cookies = new HashMap<>();
    cookies.put("qe-session", "bad");
    body = given()
            .cookies(cookies)
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract()
            .body().asString();
    logger.debug("History: {}", body);
  }

}
