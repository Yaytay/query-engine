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
package uk.co.spudsoft.query.exec.conditions;

import uk.co.spudsoft.query.exec.context.RequestContext;
import inet.ipaddr.IPAddressString;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.defn.Condition;

/**
 *
 * @author njt
 */
public class JexlEvaluatorTest {

  @Test
  public void testDefaultValueExpressions() {
    
    
    MultiMap params = HeadersMultiMap.httpHeaders();
    MultiMap headers = HeadersMultiMap.httpHeaders();
    Set<Cookie> cookies = new HashSet<>();
    Jwt jwt = new Jwt(new JsonObject()
            , new JsonObject()
                    .put("groups", Arrays.asList("BigGroup", "/Department_Fred"))
                    .put("rootCustomerKey", "snooty")
                    .put("client", "OurClient")
            , "signatureBase"
            , "signature"
    );
    IPAddressString clientIp = new IPAddressString("127.0.0.2");
    
    RequestContext request = new RequestContext(null, "requestId", "http://url", "host", "path", params, headers, cookies, clientIp, jwt);
    
    assertEquals(1, new JexlEvaluator("1").evaluateAsObject(request, null));
    assertEquals(28, new JexlEvaluator("28").evaluateAsObject(request, null));
    assertEquals(Boolean.FALSE, new JexlEvaluator("andFn(true, null)").evaluateAsObject(request, null));
    assertEquals(Boolean.TRUE, new JexlEvaluator("orFn(null, true)").evaluateAsObject(request, null));
    assertEquals("snooty", new JexlEvaluator("coalesce(request.jwt.getClaim('thingCustomerKey'), request.jwt.getClaim('rootCustomerKey'))").evaluateAsObject(request, null));
    assertNull(new JexlEvaluator("coalesce(request.jwt.getClaim('thingCustomerKey'), request.jwt.getClaim('badCustomerKey'))").evaluateAsObject(request, null));
    assertEquals("Fred", new JexlEvaluator("firstMatchingStringWithPrefix(request.jwt.groups, '/Department_', true)").evaluateAsObject(request, null));
    assertEquals("/Department_Fred", new JexlEvaluator("firstMatchingStringWithPrefix(request.jwt.groups, '/Department_', false)").evaluateAsObject(request, null));
    assertNull(new JexlEvaluator("firstMatchingStringWithPrefix(request.jwt.stuff, '/Department_', false)").evaluateAsObject(request, null));
    assertEquals("OurClient", new JexlEvaluator("request.jwt.claim[\"client\"]").evaluateAsObject(request, null));
    assertEquals("Feed", new JexlEvaluator("\"Feed\"").evaluateAsObject(request, null));
    assertEquals("Feed", new JexlEvaluator("'Feed'").evaluateAsObject(request, null));
    assertNull(new JexlEvaluator("null").evaluateAsObject(request, null));
    assertEquals(LocalDate.now(), new JexlEvaluator("now().toLocalDate()").evaluateAsObject(request, null));
    assertEquals(LocalDate.now().withDayOfMonth(1).minusMonths(1), new JexlEvaluator("now().toLocalDate().withDayOfMonth(1).minusMonths(1)").evaluateAsObject(request, null));    
  }
  
  @Test
  public void testStripWhitespace() {
    assertNull(JexlEvaluator.collapseWhitespace(null));
    assertEquals("Bob", JexlEvaluator.collapseWhitespace("Bob"));
    assertEquals("Bob\\Carol", JexlEvaluator.collapseWhitespace("Bob\\Carol"));
    assertEquals("Bob Carol", JexlEvaluator.collapseWhitespace("Bob\\\n Carol"));
    assertEquals("Bob Carol", JexlEvaluator.collapseWhitespace("Bob\\\n\t \t \t \t Carol"));
    assertEquals("Bob Carol", JexlEvaluator.collapseWhitespace("Bob  \\\n\t \t \t \t Carol"));
  }
  
  @Test
  public void testIsNullOrBlank() {
    assertTrue(JexlEvaluator.isNullOrBlank(null));
    assertTrue(JexlEvaluator.isNullOrBlank(Condition.builder().build()));
    assertTrue(JexlEvaluator.isNullOrBlank(Condition.builder().expression("").build()));
    assertFalse(JexlEvaluator.isNullOrBlank(Condition.builder().expression("true").build()));
  }
  
  
}
