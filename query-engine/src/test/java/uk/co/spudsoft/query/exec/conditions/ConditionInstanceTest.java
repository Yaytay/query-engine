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
package uk.co.spudsoft.query.exec.conditions;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author jtalbut
 */
public class ConditionInstanceTest {
  
  @Test
  public void testEvaluate() {
    assertFalse(new ConditionInstance("").evaluate(null));
    assertFalse(new ConditionInstance("7").evaluate(null));
    assertTrue(new ConditionInstance("true").evaluate(null));
    assertTrue(new ConditionInstance("true || false").evaluate(null));
    assertFalse(new ConditionInstance("true && false").evaluate(null));
    assertFalse(new ConditionInstance("false && false").evaluate(null));
    assertTrue(new ConditionInstance("true || true").evaluate(null));
  }
  
  
  static MultiMap params(String uri) {
    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
    Map<String, List<String>> prms = queryStringDecoder.parameters();
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    if (!prms.isEmpty()) {
      for (Map.Entry<String, List<String>> entry: prms.entrySet()) {
        params.add(entry.getKey(), entry.getValue());
      }
    }
    return params;
  }
  
  private static final String OPENID = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"name\":\"Bob Fred\",\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testWithRequestContext() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", false, null, Collections.singletonList("aud"));
    RequestContext ctx = rcb.buildRequestContext(req).result();
    
    assertFalse(new ConditionInstance("req").evaluate(ctx));
    assertTrue(new ConditionInstance("req.jwt.hasGroup('group1')").evaluate(ctx));
    assertFalse(new ConditionInstance("req.jwt.hasGroup(\"group7\")").evaluate(ctx));
    assertTrue(new ConditionInstance("req.jwt.getClaimAsList(\"aud\").contains('security-admin-console')").evaluate(ctx));
    assertTrue(new ConditionInstance("req.jwt.audience.contains('security-admin-console')").evaluate(ctx));
    assertTrue(new ConditionInstance("req.jwt.getClaim(\"scope\") =~ '.*[^q]?qe2[^2]?.*'").evaluate(ctx));
    assertFalse(new ConditionInstance("req.jwt.getClaim(\"scope\") =~ '.*[^q]?qe3[^3]?.*'").evaluate(ctx));
    assertTrue(new ConditionInstance("req.jwt.scope.contains(\"qe2\")").evaluate(ctx));
    assertFalse(new ConditionInstance("req.jwt.scope.contains(\"qe1\")").evaluate(ctx));
    
  }
  
}
