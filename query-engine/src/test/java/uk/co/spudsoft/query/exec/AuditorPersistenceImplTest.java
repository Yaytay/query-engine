/*
 * Copyright (C) 2026 jtalbut
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import inet.ipaddr.IPAddressString;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.junit5.VertxExtension;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import uk.co.spudsoft.query.exec.Auditor.HistoryFilters;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.OperatorsInstance;
import uk.co.spudsoft.query.main.Persistence;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@NotThreadSafe // Because other threads may alter the DatabindCodec, which would break when logging
public class AuditorPersistenceImplTest {

  @Test
  public void testPrepare(Vertx vertx) throws Exception {

    Persistence audit = new Persistence();
    audit.setRetryBase(null);
    audit.setRetryIncrement(null);
    AuditorPersistenceImpl instance1 = new AuditorPersistenceImpl(vertx, null, audit, null, new OperatorsInstance(null));

    audit.setRetryBase(Duration.ofMillis(10));
    audit.setRetryIncrement(Duration.ofMillis(10));
    AuditorPersistenceImpl instance2 = new AuditorPersistenceImpl(vertx, null, audit, null, new OperatorsInstance(null));

    assertEquals("AuditorPersistenceImpl configured without datasource", assertThrows(IllegalStateException.class, () -> {
      instance2.prepare();
    }).getMessage());

    audit.setDataSource(new DataSourceConfig());
    AuditorPersistenceImpl instance3 = new AuditorPersistenceImpl(vertx, null, audit, null, new OperatorsInstance(null));

    assertEquals("AuditorPersistenceImpl configured without datasource", assertThrows(IllegalStateException.class, () -> {
      instance3.prepare();
    }).getMessage());

    Promise<Status> promise = Promise.promise();
    instance3.healthCheck(promise);
    assertTrue(promise.future().isComplete());
    assertTrue(promise.future().succeeded());
    assertFalse(promise.future().result().isOk());
  }

  // Helper method to create rules
  RateLimitRule createRule(Duration timeLimit, int concurrencyLimit, String runLimit, String byteLimit) {
    return RateLimitRule.builder()
            .scope(Arrays.asList(RateLimitScopeType.username))
            .timeLimit(timeLimit)
            .concurrencyLimit(concurrencyLimit)
            .runLimit(runLimit)
            .byteLimit(byteLimit)
            .build();
  }

  @Test
  public void testEvaluateRateLimitRule() throws Exception {
    // Test setup
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    Instant baseTime = Instant.parse("2023-06-15T10:00:00Z");
    LocalDateTime baseTimestamp = LocalDateTime.ofInstant(baseTime, ZoneOffset.UTC);
    Duration timeLimit = Duration.ofMinutes(10);

    // ====== CONCURRENCY LIMIT TESTS ======
    // Test 1: Concurrency limit not exceeded (single outstanding run)
    RateLimitRule rule1 = createRule(timeLimit, 5, null, null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule1, baseTime, 0, 1, 0, 0, baseTimestamp);
    });

    // Test 2: Concurrency limit exactly at threshold
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule1, baseTime, 0, 5, 0, 0, baseTimestamp);
    });

    // Test 3: Concurrency limit exceeded by 1
    ServiceException ex1 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule1, baseTime, 0, 6, 0, 0, baseTimestamp);
    });
    assertEquals(429, ex1.getStatusCode());
    assertEquals("Query already running, please try again later", ex1.getMessage());

    // Test 4: Concurrency limit significantly exceeded
    ServiceException ex2 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule1, baseTime, 1, 100, 0, 0, baseTimestamp);
    });
    assertEquals(429, ex2.getStatusCode());
    assertEquals("Query already running, please try again later", ex2.getMessage());

    // Test 5: Zero concurrency limit (always fails if any outstanding)
    RateLimitRule rule2 = createRule(timeLimit, 0, null, null);
    ServiceException ex3 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule2, baseTime, 2, 1, 0, 0, baseTimestamp);
    });
    assertEquals(429, ex3.getStatusCode());

    // Test 6: Zero concurrency limit with no outstanding runs (should pass)
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule2, baseTime, 2, 0, 0, 0, baseTimestamp);
    });

    // ====== RUN LIMIT TESTS ======
    // Test 7: No run limit specified (null)
    RateLimitRule rule3 = createRule(timeLimit, 10, null, null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule3, baseTime, 0, 1, 1000000, 0, baseTimestamp);
    });

    // Test 8: Run limit not exceeded
    RateLimitRule rule4 = createRule(timeLimit, 10, "100", null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule4, baseTime, 0, 1, 50, 0, baseTimestamp);
    });

    // Test 9: Run limit exactly at threshold
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule4, baseTime, 0, 1, 100, 0, baseTimestamp);
    });

    // Test 10: Run limit exceeded by 1
    ServiceException ex4 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule4, baseTime, 3, 1, 101, 0, baseTimestamp);
    });
    assertEquals(429, ex4.getStatusCode());
    assertEquals("Run too many times, please try again later", ex4.getMessage());

    // Test 11: Run limit with K multiplier
    RateLimitRule rule5 = createRule(timeLimit, 10, "5K", null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule5, baseTime, 0, 1, 4999, 0, baseTimestamp);
    });

    ServiceException ex5 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule5, baseTime, 0, 1, 5001, 0, baseTimestamp);
    });
    assertEquals(429, ex5.getStatusCode());

    // Test 12: Run limit with M multiplier
    RateLimitRule rule6 = createRule(timeLimit, 10, "2M", null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule6, baseTime, 0, 1, 1999999, 0, baseTimestamp);
    });

    ServiceException ex6 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule6, baseTime, 0, 1, 2000001, 0, baseTimestamp);
    });
    assertEquals(429, ex6.getStatusCode());

    // Test 13: Run limit with G multiplier
    RateLimitRule rule7 = createRule(timeLimit, 10, "1G", null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule7, baseTime, 0, 1, 999999999, 0, baseTimestamp);
    });

    ServiceException ex7 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule7, baseTime, 0, 1, 1000000001, 0, baseTimestamp);
    });
    assertEquals(429, ex7.getStatusCode());

    // ====== BYTE LIMIT TESTS ======
    // Test 14: No byte limit specified (null)
    RateLimitRule rule8 = createRule(timeLimit, 10, null, null);
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule8, baseTime, 0, 1, 100, 1000000000L, baseTimestamp);
    });

    // Test 15: Byte limit not exceeded
    RateLimitRule rule9 = createRule(timeLimit, 10, null, "1000");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule9, baseTime, 0, 1, 10, 500, baseTimestamp);
    });

    // Test 16: Byte limit exactly at threshold
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule9, baseTime, 0, 1, 10, 1000, baseTimestamp);
    });

    // Test 17: Byte limit exceeded by 1
    ServiceException ex8 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule9, baseTime, 4, 1, 10, 1001, baseTimestamp);
    });
    assertEquals(429, ex8.getStatusCode());
    assertEquals("Rate limit exceeded, please try again later", ex8.getMessage());

    // Test 18: Byte limit with K multiplier
    RateLimitRule rule10 = createRule(timeLimit, 10, null, "50K");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule10, baseTime, 0, 1, 10, 49999, baseTimestamp);
    });

    ServiceException ex9 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule10, baseTime, 0, 1, 10, 50001, baseTimestamp);
    });
    assertEquals(429, ex9.getStatusCode());

    // Test 19: Byte limit with M multiplier
    RateLimitRule rule11 = createRule(timeLimit, 10, null, "5M");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule11, baseTime, 0, 1, 10, 4999999, baseTimestamp);
    });

    ServiceException ex10 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule11, baseTime, 0, 1, 10, 5000001, baseTimestamp);
    });
    assertEquals(429, ex10.getStatusCode());

    // Test 20: Byte limit with G multiplier
    RateLimitRule rule12 = createRule(timeLimit, 10, null, "1G");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule12, baseTime, 0, 1, 10, 999999999L, baseTimestamp);
    });

    ServiceException ex11 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule12, baseTime, 0, 1, 10, 1000000001L, baseTimestamp);
    });
    assertEquals(429, ex11.getStatusCode());

    // ====== COMBINATION TESTS ======
    // Test 21: All limits specified, all pass
    RateLimitRule rule13 = createRule(timeLimit, 5, "100", "1M");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule13, baseTime, 0, 3, 50, 500000, baseTimestamp);
    });

    // Test 22: All limits specified, concurrency fails first
    ServiceException ex12 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule13, baseTime, 0, 10, 50, 500000, baseTimestamp);
    });
    assertEquals(429, ex12.getStatusCode());
    assertTrue(ex12.getMessage().contains("Query already running"));

    // Test 23: All limits specified, run limit fails (concurrency passes)
    ServiceException ex13 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule13, baseTime, 0, 3, 150, 500000, baseTimestamp);
    });
    assertEquals(429, ex13.getStatusCode());
    assertTrue(ex13.getMessage().contains("Run too many times"));

    // Test 24: All limits specified, byte limit fails (others pass)
    ServiceException ex14 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule13, baseTime, 0, 3, 50, 1500000, baseTimestamp);
    });
    assertEquals(429, ex14.getStatusCode());
    assertTrue(ex14.getMessage().contains("Rate limit exceeded"));

    // ====== EDGE CASES ======
    // Test 25: Zero values for runs and bytes (should always pass)
    RateLimitRule rule14 = createRule(timeLimit, 0, "1", "1");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule14, baseTime, 0, 0, 0, 0, baseTimestamp);
    });

    // Test 26: Maximum long values
    RateLimitRule rule15 = createRule(timeLimit, Integer.MAX_VALUE, String.valueOf(Long.MAX_VALUE), String.valueOf(Long.MAX_VALUE));
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule15, baseTime, 0, 1000, 1000000, 1000000000L, baseTimestamp);
    });

    // Test 27: Different index values (for logging)
    ServiceException ex15 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule13, baseTime, 99, 10, 50, 500000, baseTimestamp);
    });
    assertEquals(429, ex15.getStatusCode());

    // Test 28: Empty string limits (should be treated as null)
    RateLimitRule rule16 = createRule(timeLimit, 10, "", "");
    assertDoesNotThrow(() -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, rule16, baseTime, 0, 1, 1000000, 1000000000L, baseTimestamp);
    });

    // ====== BOUNDARY TESTS FOR SINGULAR/PLURAL LOGGING ======
    // Test 29: Single outstanding run (singular logging)
    ServiceException ex16 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 0, null, null), baseTime, 0, 1, 0, 0, baseTimestamp);
    });
    assertEquals(429, ex16.getStatusCode());

    // Test 30: Single run limit exceeded (singular logging)
    ServiceException ex17 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 10, "0", null), baseTime, 0, 1, 1, 0, baseTimestamp);
    });
    assertEquals(429, ex17.getStatusCode());

    // Test 31: Single byte exceeded (singular logging)
    ServiceException ex18 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 10, null, "0"), baseTime, 0, 1, 0, 1, baseTimestamp);
    });
    assertEquals(429, ex18.getStatusCode());

    // Test 32: Multiple outstanding runs (plural logging)
    ServiceException ex19 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 1, null, null), baseTime, 0, 2, 0, 0, baseTimestamp);
    });
    assertEquals(429, ex19.getStatusCode());

    // Test 33: Multiple runs exceeded (plural logging)
    ServiceException ex20 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 10, "1", null), baseTime, 0, 1, 2, 0, baseTimestamp);
    });
    assertEquals(429, ex20.getStatusCode());

    // Test 34: Multiple bytes exceeded (plural logging)
    ServiceException ex21 = assertThrows(ServiceException.class, () -> {
      AuditorPersistenceImpl.evaluateRateLimitRule(reqctx, createRule(timeLimit, 10, null, "1"), baseTime, 0, 1, 0, 2, baseTimestamp);
    });
    assertEquals(429, ex21.getStatusCode());
  }

  @Test
  public void testMultiMapToJson() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);

    // Test 1: Null input
    assertNull(AuditorPersistenceImpl.multiMapToJson(reqctx, null));

    // Test 2: Empty MultiMap
    MultiMap emptyMap = HeadersMultiMap.httpHeaders();
    JsonObject emptyResult = AuditorPersistenceImpl.multiMapToJson(reqctx, emptyMap);
    assertNotNull(emptyResult);
    assertEquals(0, emptyResult.size());

    // Test 3: Single key-value pair
    MultiMap singleMap = HeadersMultiMap.httpHeaders().add("key1", "value1");
    JsonObject singleResult = AuditorPersistenceImpl.multiMapToJson(reqctx, singleMap);
    assertEquals(1, singleResult.size());
    assertEquals("value1", singleResult.getString("key1"));

    // Test 4: Multiple different keys
    MultiMap multipleKeysMap = HeadersMultiMap.httpHeaders()
            .add("key1", "value1")
            .add("key2", "value2")
            .add("key3", "value3");
    JsonObject multipleKeysResult = AuditorPersistenceImpl.multiMapToJson(reqctx, multipleKeysMap);
    assertEquals(3, multipleKeysResult.size());
    assertEquals("value1", multipleKeysResult.getString("key1"));
    assertEquals("value2", multipleKeysResult.getString("key2"));
    assertEquals("value3", multipleKeysResult.getString("key3"));

    // Test 5: Duplicate keys (should create JsonArray)
    MultiMap duplicateKeysMap = HeadersMultiMap.httpHeaders()
            .add("duplicate", "first")
            .add("duplicate", "second");
    JsonObject duplicateKeysResult = AuditorPersistenceImpl.multiMapToJson(reqctx, duplicateKeysMap);
    assertEquals(1, duplicateKeysResult.size());
    JsonArray expectedArray = new JsonArray().add("first").add("second");
    assertEquals(expectedArray, duplicateKeysResult.getJsonArray("duplicate"));

    // Test 6: Multiple values for same key (more than 2)
    MultiMap multipleValuesMap = HeadersMultiMap.httpHeaders()
            .add("multi", "first")
            .add("multi", "second")
            .add("multi", "third")
            .add("multi", "fourth");
    JsonObject multipleValuesResult = AuditorPersistenceImpl.multiMapToJson(reqctx, multipleValuesMap);
    assertEquals(1, multipleValuesResult.size());
    JsonArray expectedMultiArray = new JsonArray().add("first").add("second").add("third").add("fourth");
    assertEquals(expectedMultiArray, multipleValuesResult.getJsonArray("multi"));

    // Test 7: Authorization header with Bearer token (should be protected)
    MultiMap authBearerMap = HeadersMultiMap.httpHeaders()
            .add(HttpHeaders.AUTHORIZATION.toString(), "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
    JsonObject authBearerResult = AuditorPersistenceImpl.multiMapToJson(reqctx, authBearerMap);
    assertEquals(1, authBearerResult.size());
    String protectedBearer = authBearerResult.getString(HttpHeaders.AUTHORIZATION.toString());
    assertTrue(protectedBearer.startsWith("Bearer "));
    assertTrue(protectedBearer.contains("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"));
    assertFalse(protectedBearer.contains("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));

    // Test 8: Authorization header with Basic auth (should be protected)
    MultiMap authBasicMap = HeadersMultiMap.httpHeaders()
            .add(HttpHeaders.AUTHORIZATION.toString(), "Basic dXNlcm5hbWU6cGFzc3dvcmQ=");
    JsonObject authBasicResult = AuditorPersistenceImpl.multiMapToJson(reqctx, authBasicMap);
    assertEquals(1, authBasicResult.size());
    String protectedBasic = authBasicResult.getString(HttpHeaders.AUTHORIZATION.toString());
    assertTrue(protectedBasic.startsWith("Basic "));
    assertFalse(protectedBasic.contains("dXNlcm5hbWU6cGFzc3dvcmQ="));

    // Test 9: Authorization header case-insensitive
    MultiMap authCaseMap = MultiMap.caseInsensitiveMultiMap()
            .add("authorization", "Bearer token.here.secret")
            .add("AUTHORIZATION", "Basic secret");
    JsonObject authCaseResult = AuditorPersistenceImpl.multiMapToJson(reqctx, authCaseMap);
    assertEquals(1, authCaseResult.size());
    JsonArray authArray = authCaseResult.getJsonArray("authorization");
    assertNotNull(authArray);
    assertEquals(2, authArray.size());
    assertEquals("Bearer token.here", authArray.getString(0));
    assertEquals("Basic secret", authArray.getString(1)); // This isn't valid basic auth so it hasn't been protected

    // Test 10: Mixed scenario - single values, multiple values, and auth headers
    MultiMap mixedMap = HeadersMultiMap.httpHeaders()
            .add("single", "value")
            .add("multiple", "first")
            .add("multiple", "second")
            .add(HttpHeaders.AUTHORIZATION.toString(), "Bearer secret.token.here")
            .add("Content-Type", "application/json")
            .add("Accept", "application/json")
            .add("Accept", "text/html");
    JsonObject mixedResult = AuditorPersistenceImpl.multiMapToJson(reqctx, mixedMap);
    assertEquals(5, mixedResult.size());

    // Verify single value
    assertEquals("value", mixedResult.getString("single"));

    // Verify multiple values
    JsonArray multipleArray = mixedResult.getJsonArray("multiple");
    assertEquals(2, multipleArray.size());
    assertEquals("first", multipleArray.getString(0));
    assertEquals("second", multipleArray.getString(1));

    // Verify protected auth header
    String authValue = mixedResult.getString(HttpHeaders.AUTHORIZATION.toString());
    assertEquals("Bearer secret.token", authValue);

    // Verify other single value
    assertEquals("application/json", mixedResult.getString("Content-Type"));

    // Verify other multiple values
    JsonArray acceptArray = mixedResult.getJsonArray("Accept");
    assertEquals(2, acceptArray.size());
    assertEquals("application/json", acceptArray.getString(0));
    assertEquals("text/html", acceptArray.getString(1));

    // Test 11: Empty string values
    MultiMap emptyValueMap = HeadersMultiMap.httpHeaders()
            .add("empty", "")
            .add("null-like", "null")
            .add("empty", "not-empty");
    JsonObject emptyValueResult = AuditorPersistenceImpl.multiMapToJson(reqctx, emptyValueMap);
    assertEquals(2, emptyValueResult.size());
    JsonArray emptyArray = emptyValueResult.getJsonArray("empty");
    assertEquals(2, emptyArray.size());
    assertEquals("", emptyArray.getString(0));
    assertEquals("not-empty", emptyArray.getString(1));
    assertEquals("null", emptyValueResult.getString("null-like"));

    // Test 12: Special characters in keys and values
    MultiMap specialCharsMap = HeadersMultiMap.httpHeaders()
            .add("key-with-dashes", "value with spaces")
            .add("key_with_underscores", "value@with#special$chars")
            .add("UPPERCASE_KEY", "MixedCaseValue");
    JsonObject specialCharsResult = AuditorPersistenceImpl.multiMapToJson(reqctx, specialCharsMap);
    assertEquals(3, specialCharsResult.size());
    assertEquals("value with spaces", specialCharsResult.getString("key-with-dashes"));
    assertEquals("value@with#special$chars", specialCharsResult.getString("key_with_underscores"));
    assertEquals("MixedCaseValue", specialCharsResult.getString("UPPERCASE_KEY"));
  }

  @Test
  public void testProtectAuthHeader() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    Base64.Encoder encoder = Base64.getEncoder();

    // ====== NULL AND EMPTY TESTS ======
    // Test 1: Null input
    assertNull(AuditorPersistenceImpl.protectAuthHeader(reqctx, null));

    // Test 2: Empty string
    assertEquals("", AuditorPersistenceImpl.protectAuthHeader(reqctx, ""));

    // Test 3: Whitespace only
    assertEquals("   ", AuditorPersistenceImpl.protectAuthHeader(reqctx, "   "));

    // ====== NON-AUTH HEADER TESTS ======
    // Test 4: Random string (no Basic or Bearer prefix)
    assertEquals("random-value", AuditorPersistenceImpl.protectAuthHeader(reqctx, "random-value"));

    // Test 5: String that contains but doesn't start with Basic
    assertEquals("Not Basic auth", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Not Basic auth"));

    // Test 6: String that contains but doesn't start with Bearer
    assertEquals("Not Bearer token", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Not Bearer token"));

    // Test 7: Case sensitivity - lowercase basic
    assertEquals("basic dXNlcjpwYXNz", AuditorPersistenceImpl.protectAuthHeader(reqctx, "basic dXNlcjpwYXNz"));

    // Test 8: Case sensitivity - lowercase bearer
    assertEquals("bearer token.here.secret", AuditorPersistenceImpl.protectAuthHeader(reqctx, "bearer token.here.secret"));

    // ====== BASIC AUTH TESTS ======
    // Test 9: Valid Basic auth with username:password
    String userPassBase64 = encoder.encodeToString("username:password".getBytes(StandardCharsets.UTF_8));
    String basicAuth = "Basic " + userPassBase64;
    String protectedBasic = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuth);
    String expectedUserBase64 = encoder.encodeToString("username:".getBytes(StandardCharsets.UTF_8));
    assertEquals("Basic " + expectedUserBase64, protectedBasic);

    // Test 10: Basic auth with just username (no colon)
    String userOnlyBase64 = encoder.encodeToString("username".getBytes(StandardCharsets.UTF_8));
    String basicAuthNoColon = "Basic " + userOnlyBase64;
    String protectedBasicNoColon = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuthNoColon);
    assertEquals(basicAuthNoColon, protectedBasicNoColon); // Should remain unchanged

    // Test 11: Basic auth with empty username
    String emptyUserBase64 = encoder.encodeToString(":password".getBytes(StandardCharsets.UTF_8));
    String basicAuthEmptyUser = "Basic " + emptyUserBase64;
    String protectedBasicEmptyUser = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuthEmptyUser);
    assertEquals(basicAuthEmptyUser, protectedBasicEmptyUser); // Should remain unchanged (colonPos <= 0)

    // Test 12: Basic auth with colon at start
    String colonAtStartBase64 = encoder.encodeToString(":password".getBytes(StandardCharsets.UTF_8));
    String basicAuthColonStart = "Basic " + colonAtStartBase64;
    assertEquals(basicAuthColonStart, AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuthColonStart));

    // Test 13: Basic auth with multiple colons (should take first)
    String multiColonBase64 = encoder.encodeToString("user:pass:word".getBytes(StandardCharsets.UTF_8));
    String basicAuthMultiColon = "Basic " + multiColonBase64;
    String protectedMultiColon = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuthMultiColon);
    String expectedMultiColonUser = encoder.encodeToString("user:".getBytes(StandardCharsets.UTF_8));
    assertEquals("Basic " + expectedMultiColonUser, protectedMultiColon);

    // Test 14: Basic auth with special characters in username
    String specialUserBase64 = encoder.encodeToString("user@domain.com:password".getBytes(StandardCharsets.UTF_8));
    String basicAuthSpecial = "Basic " + specialUserBase64;
    String protectedSpecial = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicAuthSpecial);
    String expectedSpecialUser = encoder.encodeToString("user@domain.com:".getBytes(StandardCharsets.UTF_8));
    assertEquals("Basic " + expectedSpecialUser, protectedSpecial);

    // Test 15: Basic with no space after "Basic"
    assertEquals("Basic", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Basic"));

    // Test 16: Basic with only space after "Basic"
    assertEquals("Basic ", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Basic "));

    // Test 17: Basic with invalid base64
    String invalidBase64 = "Basic invalid-base64!@#";
    assertEquals(invalidBase64, AuditorPersistenceImpl.protectAuthHeader(reqctx, invalidBase64)); // Should remain unchanged due to exception handling

    // ====== BEARER TOKEN TESTS ======
    // Test 18: Valid JWT token (3 parts)
    String jwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String protectedJwt = AuditorPersistenceImpl.protectAuthHeader(reqctx, jwtToken);
    assertEquals("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ", protectedJwt);

    // Test 19: Bearer token with 2 parts (header.payload, no signature)
    String twoPartToken = "Bearer header.payload";
    String protectedTwoPart = AuditorPersistenceImpl.protectAuthHeader(reqctx, twoPartToken);
    assertEquals("Bearer header", protectedTwoPart);

    // Test 20: Bearer token with multiple dots (should take last dot)
    String multiDotToken = "Bearer part1.part2.part3.part4.signature";
    String protectedMultiDot = AuditorPersistenceImpl.protectAuthHeader(reqctx, multiDotToken);
    assertEquals("Bearer part1.part2.part3.part4", protectedMultiDot);

    // Test 21: Bearer token with no dots
    String noDotToken = "Bearer simpletoken";
    String protectedNoDot = AuditorPersistenceImpl.protectAuthHeader(reqctx, noDotToken);
    assertEquals(noDotToken, protectedNoDot); // Should remain unchanged (no dots found)

    // Test 22: Bearer with no space after "Bearer"
    assertEquals("Bearer", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Bearer"));

    // Test 23: Bearer with only space after "Bearer"
    assertEquals("Bearer ", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Bearer "));

    // Test 24: Bearer token ending with dot
    String tokenEndingDot = "Bearer token.";
    String protectedEndingDot = AuditorPersistenceImpl.protectAuthHeader(reqctx, tokenEndingDot);
    assertEquals("Bearer token", protectedEndingDot);

    // Test 25: Bearer token starting with dot
    String tokenStartingDot = "Bearer .token";
    String protectedStartingDot = AuditorPersistenceImpl.protectAuthHeader(reqctx, tokenStartingDot);
    assertEquals("Bearer ", protectedStartingDot);

    // ====== EDGE CASES ======
    // Test 26: Basic followed by Bearer (should process as Basic)
    String basicBearer = "Basic Bearer.token.here";
    assertEquals(basicBearer, AuditorPersistenceImpl.protectAuthHeader(reqctx, basicBearer)); // Invalid base64, should remain unchanged

    // Test 27: Bearer followed by Basic (should process as Bearer)
    String bearerBasic = "Bearer Basic.auth.here";
    String protectedBearerBasic = AuditorPersistenceImpl.protectAuthHeader(reqctx, bearerBasic);
    assertEquals("Bearer Basic.auth", protectedBearerBasic);

    // Test 28: Very long strings
    StringBuilder longToken = new StringBuilder("Bearer ");
    for (int i = 0; i < 1000; i++) {
      longToken.append("a");
    }
    longToken.append(".signature");
    String protectedLong = AuditorPersistenceImpl.protectAuthHeader(reqctx, longToken.toString());
    assertFalse(protectedLong.contains(".signature"));
    assertTrue(protectedLong.startsWith("Bearer "));

    // Test 29: Unicode characters in Basic auth
    String unicodeBase64 = encoder.encodeToString("用户:密码".getBytes(StandardCharsets.UTF_8));
    String basicUnicode = "Basic " + unicodeBase64;
    String protectedUnicode = AuditorPersistenceImpl.protectAuthHeader(reqctx, basicUnicode);
    String expectedUnicodeUser = encoder.encodeToString("用户:".getBytes(StandardCharsets.UTF_8));
    assertEquals("Basic " + expectedUnicodeUser, protectedUnicode);

    // Test 30: Exact prefix matches
    assertEquals("Basic", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Basic"));
    assertEquals("Bearer", AuditorPersistenceImpl.protectAuthHeader(reqctx, "Bearer"));
    assertEquals("BasicAuth", AuditorPersistenceImpl.protectAuthHeader(reqctx, "BasicAuth")); // Not exact match
    assertEquals("BearerToken", AuditorPersistenceImpl.protectAuthHeader(reqctx, "BearerToken")); // Not exact match
  }

  @Test
  public void testGetHistory() {
  }

  @Test
  public void testGetInteger() throws SQLException {
    // ====== VALID INTEGER TESTS ======

    // Test 1: Valid positive integer
    ResultSet rs1 = mock(ResultSet.class);
    when(rs1.getInt(1)).thenReturn(42);
    when(rs1.wasNull()).thenReturn(false);
    Integer result1 = AuditorPersistenceImpl.getInteger(rs1, 1);
    assertEquals(Integer.valueOf(42), result1);

    // Test 2: Valid negative integer
    ResultSet rs2 = mock(ResultSet.class);
    when(rs2.getInt(2)).thenReturn(-100);
    when(rs2.wasNull()).thenReturn(false);
    Integer result2 = AuditorPersistenceImpl.getInteger(rs2, 2);
    assertEquals(Integer.valueOf(-100), result2);

    // Test 3: Zero value
    ResultSet rs3 = mock(ResultSet.class);
    when(rs3.getInt(1)).thenReturn(0);
    when(rs3.wasNull()).thenReturn(false);
    Integer result3 = AuditorPersistenceImpl.getInteger(rs3, 1);
    assertEquals(Integer.valueOf(0), result3);

    // Test 4: Maximum integer value
    ResultSet rs4 = mock(ResultSet.class);
    when(rs4.getInt(1)).thenReturn(Integer.MAX_VALUE);
    when(rs4.wasNull()).thenReturn(false);
    Integer result4 = AuditorPersistenceImpl.getInteger(rs4, 1);
    assertEquals(Integer.valueOf(Integer.MAX_VALUE), result4);

    // Test 5: Minimum integer value
    ResultSet rs5 = mock(ResultSet.class);
    when(rs5.getInt(1)).thenReturn(Integer.MIN_VALUE);
    when(rs5.wasNull()).thenReturn(false);
    Integer result5 = AuditorPersistenceImpl.getInteger(rs5, 1);
    assertEquals(Integer.valueOf(Integer.MIN_VALUE), result5);

    // ====== NULL VALUE TESTS ======
    // Test 6: NULL database value
    ResultSet rs6 = mock(ResultSet.class);
    when(rs6.getInt(1)).thenReturn(0); // JDBC returns 0 for NULL integers
    when(rs6.wasNull()).thenReturn(true);
    Integer result6 = AuditorPersistenceImpl.getInteger(rs6, 1);
    assertNull(result6);

    // Test 7: NULL with different column index
    ResultSet rs7 = mock(ResultSet.class);
    when(rs7.getInt(5)).thenReturn(123); // Should be ignored due to wasNull() = true
    when(rs7.wasNull()).thenReturn(true);
    Integer result7 = AuditorPersistenceImpl.getInteger(rs7, 5);
    assertNull(result7);

    // ====== EXCEPTION TESTS ======
    // Test 8: SQLException from getInt
    ResultSet rs8 = mock(ResultSet.class);
    when(rs8.getInt(1)).thenThrow(new SQLException("Column not found"));
    assertThrows(SQLException.class, () -> {
      AuditorPersistenceImpl.getInteger(rs8, 1);
    });

    // Test 9: SQLException from wasNull
    ResultSet rs9 = mock(ResultSet.class);
    when(rs9.getInt(1)).thenReturn(42);
    when(rs9.wasNull()).thenThrow(new SQLException("Connection lost"));
    assertThrows(SQLException.class, () -> {
      AuditorPersistenceImpl.getInteger(rs9, 1);
    });

    // ====== DIFFERENT COLUMN INDICES ======
    // Test 10: Column index 0 (edge case)
    ResultSet rs10 = mock(ResultSet.class);
    when(rs10.getInt(0)).thenReturn(999);
    when(rs10.wasNull()).thenReturn(false);
    Integer result10 = AuditorPersistenceImpl.getInteger(rs10, 0);
    assertEquals(Integer.valueOf(999), result10);

    // Test 11: Large column index
    ResultSet rs11 = mock(ResultSet.class);
    when(rs11.getInt(100)).thenReturn(777);
    when(rs11.wasNull()).thenReturn(false);
    Integer result11 = AuditorPersistenceImpl.getInteger(rs11, 100);
    assertEquals(Integer.valueOf(777), result11);
  }

  @Test
  public void testGetLong() throws SQLException {
    // ====== VALID LONG TESTS ======

    // Test 1: Valid positive long
    ResultSet rs1 = mock(ResultSet.class);
    when(rs1.getLong(1)).thenReturn(42L);
    when(rs1.wasNull()).thenReturn(false);
    Long result1 = AuditorPersistenceImpl.getLong(rs1, 1);
    assertEquals(Long.valueOf(42L), result1);

    // Test 2: Valid negative long
    ResultSet rs2 = mock(ResultSet.class);
    when(rs2.getLong(2)).thenReturn(-100L);
    when(rs2.wasNull()).thenReturn(false);
    Long result2 = AuditorPersistenceImpl.getLong(rs2, 2);
    assertEquals(Long.valueOf(-100L), result2);

    // Test 3: Zero value
    ResultSet rs3 = mock(ResultSet.class);
    when(rs3.getLong(1)).thenReturn(0L);
    when(rs3.wasNull()).thenReturn(false);
    Long result3 = AuditorPersistenceImpl.getLong(rs3, 1);
    assertEquals(Long.valueOf(0L), result3);

    // Test 4: Maximum long value
    ResultSet rs4 = mock(ResultSet.class);
    when(rs4.getLong(1)).thenReturn(Long.MAX_VALUE);
    when(rs4.wasNull()).thenReturn(false);
    Long result4 = AuditorPersistenceImpl.getLong(rs4, 1);
    assertEquals(Long.valueOf(Long.MAX_VALUE), result4);

    // Test 5: Minimum long value
    ResultSet rs5 = mock(ResultSet.class);
    when(rs5.getLong(1)).thenReturn(Long.MIN_VALUE);
    when(rs5.wasNull()).thenReturn(false);
    Long result5 = AuditorPersistenceImpl.getLong(rs5, 1);
    assertEquals(Long.valueOf(Long.MIN_VALUE), result5);

    // Test 6: Large positive value beyond int range
    ResultSet rs6 = mock(ResultSet.class);
    long largeValue = ((long) Integer.MAX_VALUE) + 1000L;
    when(rs6.getLong(1)).thenReturn(largeValue);
    when(rs6.wasNull()).thenReturn(false);
    Long result6 = AuditorPersistenceImpl.getLong(rs6, 1);
    assertEquals(Long.valueOf(largeValue), result6);

    // Test 7: Large negative value beyond int range
    ResultSet rs7 = mock(ResultSet.class);
    long largeNegValue = ((long) Integer.MIN_VALUE) - 1000L;
    when(rs7.getLong(1)).thenReturn(largeNegValue);
    when(rs7.wasNull()).thenReturn(false);
    Long result7 = AuditorPersistenceImpl.getLong(rs7, 1);
    assertEquals(Long.valueOf(largeNegValue), result7);

    // ====== NULL VALUE TESTS ======
    // Test 8: NULL database value
    ResultSet rs8 = mock(ResultSet.class);
    when(rs8.getLong(1)).thenReturn(0L); // JDBC returns 0 for NULL longs
    when(rs8.wasNull()).thenReturn(true);
    Long result8 = AuditorPersistenceImpl.getLong(rs8, 1);
    assertNull(result8);

    // Test 9: NULL with different column index
    ResultSet rs9 = mock(ResultSet.class);
    when(rs9.getLong(5)).thenReturn(123456789L); // Should be ignored due to wasNull() = true
    when(rs9.wasNull()).thenReturn(true);
    Long result9 = AuditorPersistenceImpl.getLong(rs9, 5);
    assertNull(result9);

    // ====== EXCEPTION TESTS ======
    // Test 10: SQLException from getLong
    ResultSet rs10 = mock(ResultSet.class);
    when(rs10.getLong(1)).thenThrow(new SQLException("Column not found"));
    assertThrows(SQLException.class, () -> {
      AuditorPersistenceImpl.getLong(rs10, 1);
    });

    // Test 11: SQLException from wasNull
    ResultSet rs11 = mock(ResultSet.class);
    when(rs11.getLong(1)).thenReturn(42L);
    when(rs11.wasNull()).thenThrow(new SQLException("Connection lost"));
    assertThrows(SQLException.class, () -> {
      AuditorPersistenceImpl.getLong(rs11, 1);
    });

    // ====== DIFFERENT COLUMN INDICES ======
    // Test 12: Column index 0 (edge case)
    ResultSet rs12 = mock(ResultSet.class);
    when(rs12.getLong(0)).thenReturn(999L);
    when(rs12.wasNull()).thenReturn(false);
    Long result12 = AuditorPersistenceImpl.getLong(rs12, 0);
    assertEquals(Long.valueOf(999L), result12);

    // Test 13: Large column index
    ResultSet rs13 = mock(ResultSet.class);
    when(rs13.getLong(100)).thenReturn(777L);
    when(rs13.wasNull()).thenReturn(false);
    Long result13 = AuditorPersistenceImpl.getLong(rs13, 100);
    assertEquals(Long.valueOf(777L), result13);
  }

  @Test
  public void testGetArguments(Vertx vertx) throws SQLException {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);

    // ====== VALID JSON TESTS ======
    // Test 1: Valid simple JSON object
    ResultSet rs1 = mock(ResultSet.class);
    String validJson = "{\"key1\":\"value1\",\"key2\":123}";
    when(rs1.getString(1)).thenReturn(validJson);
    when(rs1.wasNull()).thenReturn(false);

    Persistence audit = new Persistence();
    AuditorPersistenceImpl instance = new AuditorPersistenceImpl(vertx, null, audit, null, new OperatorsInstance(null));

    ObjectNode result1 = instance.getArguments(reqctx, rs1, 1, "test-id-1");
    assertNotNull(result1);
    assertEquals("value1", result1.get("key1").asText());
    assertEquals(123, result1.get("key2").asInt());

    // Test 2: Empty JSON object
    ResultSet rs2 = mock(ResultSet.class);
    when(rs2.getString(1)).thenReturn("{}");
    when(rs2.wasNull()).thenReturn(false);

    ObjectNode result2 = instance.getArguments(reqctx, rs2, 1, "test-id-2");
    assertNotNull(result2);
    assertEquals(0, result2.size());

    // Test 3: Complex nested JSON
    ResultSet rs3 = mock(ResultSet.class);
    String complexJson = "{\"user\":{\"name\":\"John\",\"age\":30},\"items\":[\"a\",\"b\",\"c\"]}";
    when(rs3.getString(1)).thenReturn(complexJson);
    when(rs3.wasNull()).thenReturn(false);

    ObjectNode result3 = instance.getArguments(reqctx, rs3, 1, "test-id-3");
    assertNotNull(result3);
    assertEquals("John", result3.get("user").get("name").asText());
    assertEquals(30, result3.get("user").get("age").asInt());
    assertTrue(result3.get("items").isArray());
    assertEquals(3, result3.get("items").size());

    // Test 4: JSON with null values
    ResultSet rs4 = mock(ResultSet.class);
    String jsonWithNulls = "{\"key1\":null,\"key2\":\"value2\"}";
    when(rs4.getString(1)).thenReturn(jsonWithNulls);
    when(rs4.wasNull()).thenReturn(false);

    ObjectNode result4 = instance.getArguments(reqctx, rs4, 1, "test-id-4");
    assertNotNull(result4);
    assertTrue(result4.get("key1").isNull());
    assertEquals("value2", result4.get("key2").asText());

    // ====== NULL/EMPTY TESTS ======
    // Test 5: NULL database value
    ResultSet rs5 = mock(ResultSet.class);
    when(rs5.getString(1)).thenReturn(null);
    when(rs5.wasNull()).thenReturn(true);

    ObjectNode result5 = instance.getArguments(reqctx, rs5, 1, "test-id-5");
    assertNull(result5);

    // Test 6: Empty string
    ResultSet rs6 = mock(ResultSet.class);
    when(rs6.getString(1)).thenReturn("");
    when(rs6.wasNull()).thenReturn(false);

    ObjectNode result6 = instance.getArguments(reqctx, rs6, 1, "test-id-6");
    assertNull(result6);

    // Test 7: Whitespace only string
    ResultSet rs7 = mock(ResultSet.class);
    when(rs7.getString(1)).thenReturn("   \t\n  ");
    when(rs7.wasNull()).thenReturn(false);

    ObjectNode result7 = instance.getArguments(reqctx, rs7, 1, "test-id-7");
    assertNull(result7);

    // ====== INVALID JSON TESTS ======
    // Test 8: Invalid JSON syntax
    ResultSet rs8 = mock(ResultSet.class);
    when(rs8.getString(1)).thenReturn("{invalid json}");
    when(rs8.wasNull()).thenReturn(false);

    ObjectNode result8 = instance.getArguments(reqctx, rs8, 1, "test-id-8");
    assertNull(result8); // Should return null on parse failure

    // Test 9: JSON array instead of object
    ResultSet rs9 = mock(ResultSet.class);
    when(rs9.getString(1)).thenReturn("[\"item1\",\"item2\"]");
    when(rs9.wasNull()).thenReturn(false);

    ObjectNode result9 = instance.getArguments(reqctx, rs9, 1, "test-id-9");
    assertNull(result9); // Should return null as it's not an object

    // Test 10: JSON primitive value
    ResultSet rs10 = mock(ResultSet.class);
    when(rs10.getString(1)).thenReturn("\"just a string\"");
    when(rs10.wasNull()).thenReturn(false);

    ObjectNode result10 = instance.getArguments(reqctx, rs10, 1, "test-id-10");
    assertNull(result10); // Should return null as it's not an object

    // Test 11: Malformed JSON - missing quotes
    ResultSet rs11 = mock(ResultSet.class);
    when(rs11.getString(1)).thenReturn("{key: value}");
    when(rs11.wasNull()).thenReturn(false);

    ObjectNode result11 = instance.getArguments(reqctx, rs11, 1, "test-id-11");
    assertNull(result11);

    // Test 12: Malformed JSON - trailing comma
    ResultSet rs12 = mock(ResultSet.class);
    when(rs12.getString(1)).thenReturn("{\"key1\":\"value1\",}");
    when(rs12.wasNull()).thenReturn(false);

    ObjectNode result12 = instance.getArguments(reqctx, rs12, 1, "test-id-12");
    assertNull(result12);

    // ====== SPECIAL CHARACTERS TESTS ======
    // Test 13: JSON with special characters
    ResultSet rs13 = mock(ResultSet.class);
    String specialCharsJson = "{\"unicode\":\"こんにちは\",\"escape\":\"line1\\nline2\\ttab\",\"quote\":\"say \\\"hello\\\"\"}";
    when(rs13.getString(1)).thenReturn(specialCharsJson);
    when(rs13.wasNull()).thenReturn(false);

    ObjectNode result13 = instance.getArguments(reqctx, rs13, 1, "test-id-13");
    assertNotNull(result13);
    assertEquals("こんにちは", result13.get("unicode").asText());
    assertEquals("line1\nline2\ttab", result13.get("escape").asText());
    assertEquals("say \"hello\"", result13.get("quote").asText());

    // ====== EXCEPTION TESTS ======
    // Test 14: SQLException from getString
    ResultSet rs14 = mock(ResultSet.class);
    when(rs14.getString(1)).thenThrow(new SQLException("Column not found"));

    assertThrows(SQLException.class, () -> {
      instance.getArguments(reqctx, rs14, 1, "test-id-14");
    });

    // Test 15: SQLException from wasNull
    ResultSet rs15 = mock(ResultSet.class);
    when(rs15.getString(1)).thenReturn("{}");
    when(rs15.wasNull()).thenThrow(new SQLException("Connection lost"));

    assertThrows(SQLException.class, () -> {
      instance.getArguments(reqctx, rs15, 1, "test-id-15");
    });

    // ====== EDGE CASES ======
    // Test 16: Very large JSON object
    ResultSet rs16 = mock(ResultSet.class);
    StringBuilder largeJson = new StringBuilder("{");
    for (int i = 0; i < 1000; i++) {
      if (i > 0) {
        largeJson.append(",");
      }
      largeJson.append("\"key").append(i).append("\":\"value").append(i).append("\"");
    }
    largeJson.append("}");
    when(rs16.getString(1)).thenReturn(largeJson.toString());
    when(rs16.wasNull()).thenReturn(false);

    ObjectNode result16 = instance.getArguments(reqctx, rs16, 1, "test-id-16");
    assertNotNull(result16);
    assertEquals(1000, result16.size());
    assertEquals("value0", result16.get("key0").asText());
    assertEquals("value999", result16.get("key999").asText());

    // Test 17: Different column indices
    ResultSet rs17 = mock(ResultSet.class);
    when(rs17.getString(5)).thenReturn("{\"test\":\"column5\"}");
    when(rs17.wasNull()).thenReturn(false);

    ObjectNode result17 = instance.getArguments(reqctx, rs17, 5, "test-id-17");
    assertNotNull(result17);
    assertEquals("column5", result17.get("test").asText());

    // Test 18: Different ID values (for logging)
    ResultSet rs18 = mock(ResultSet.class);
    when(rs18.getString(1)).thenReturn("{invalid");
    when(rs18.wasNull()).thenReturn(false);

    ObjectNode result18 = instance.getArguments(reqctx, rs18, 1, "special-id-123");
    assertNull(result18); // Should handle invalid JSON gracefully regardless of ID
  }

  private final String quote = "\"";

  @Test
  void buildHistoryRowsFilter_GlobalOperator_AppendsAlwaysTrue() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(true, false);
    HistoryFilters filters = new HistoryFilters(null, null, null, null, null, null);

    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "iss", "sub", filters);
    
    assertThat(filterBuilder.toString(), equalTo(" 1 = 1 "));
    assertThat(args, empty());
  }

  @Test
  void buildHistoryRowsFilter_ClientOperator_FiltersByIssuerOnly() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(false, true);
    HistoryFilters filters = new HistoryFilters(null, null, null, null, null, null);

    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "my-issuer", "sub", filters);

    assertThat(filterBuilder.toString(), equalTo(" \"issuer\" = ? "));
    assertThat(args, equalTo(Arrays.asList("my-issuer")));
  }

  @Test
  void buildHistoryRowsFilter_RegularUser_FiltersByIssuerAndSubject() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(false, false);
    HistoryFilters filters = new HistoryFilters(null, null, null, null, null, null);

    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "iss", "sub", filters);

    assertThat(filterBuilder.toString(), equalTo(" \"issuer\" = ?  and \"subject\" = ? "));
    assertThat(args, equalTo(Arrays.asList("iss", "sub")));
  }

  @Test
  void buildHistoryRowsFilter_WithAllFilters_AppendsCorrectClausesAndArgs() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(true, false);
    LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 2, 10, 0);
    HistoryFilters filters = new HistoryFilters(start, end, "filt-iss", "filt-name", "/api/data", 200);


    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "iss", "sub", filters);

    assertThat(filterBuilder.toString(), equalTo(" 1 = 1  and \"timestamp\" > ?  and \"timestamp\" <= ?  and \"issuer\" = ?  and \"name\" = ?  and \"path\" like ?  and \"responseCode\" = ? "));
    assertThat(args, equalTo(Arrays.asList(start, end, "filt-iss", "filt-name", "/api/data%", 200)));
  }

  @Test
  void buildHistoryRowsFilter_PathEscaping_HandlesSpecialSqlCharacters() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(true, false);
    HistoryFilters filters = new HistoryFilters(null, null, null, null, "a_b%c\\d", null);

    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "iss", "sub", filters);

    // Expected escaping: \ -> \\, % -> \%, _ -> \_ and suffix %
    assertTrue(args.contains("a\\_b\\%c\\\\d%"));
  }

  @Test
  void buildHistoryRowsFilter_NullFilters_DoesNotAppendExtraClauses() {
    StringBuilder filterBuilder = new StringBuilder();
    List<Object> args = new ArrayList<>();
    OperatorsInstance.Flags flags = new OperatorsInstance.Flags(true, false);
    HistoryFilters filters = new HistoryFilters(null, null, null, null, null, null);

    AuditorPersistenceImpl.buildHistoryRowsFilter(flags, filterBuilder, args, quote, "iss", "sub", filters);

    assertThat(filterBuilder.toString(), equalTo(" 1 = 1 "));
    assertThat(args, empty());
  }
}
