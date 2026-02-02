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

import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.Authenticator;
import com.google.common.collect.ImmutableMap;
import inet.ipaddr.IPAddressString;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.RequestContextTest;
import uk.co.spudsoft.query.web.LoginDaoMemoryImpl;

/**
 *
 * @author jtalbut
 */
public class ConditionInstanceTest {

  @Test
  public void testEvaluate() {
    assertFalse(new ConditionInstance("").evaluate(null, null));
    assertFalse(new ConditionInstance("7").evaluate(null, null));
    assertTrue(new ConditionInstance("true").evaluate(null, null));
    assertTrue(new ConditionInstance("true || false").evaluate(null, null));
    assertFalse(new ConditionInstance("true && false").evaluate(null, null));
    assertFalse(new ConditionInstance("false && false").evaluate(null, null));
    assertTrue(new ConditionInstance("true || true").evaluate(null, null));
  }


  private static final String OPENID = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"name\":\"Bob Fred\",\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  private static final String OPENID2 = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":[\"service-query-engine\", \"client-root-bob.fred.net\", \"client-root-bob.carol.net\", \"thingummy\"],\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"name\":\"Bob Fred\",\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
    

  @Test
  public void testWithRequestContext() {
    ch.qos.logback.classic.Logger lg = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ConditionInstance.class);
    ch.qos.logback.classic.Level origLvl = lg.getLevel();
    lg.setLevel(ch.qos.logback.classic.Level.DEBUG);
    
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID);
    when(req.params()).thenReturn(RequestContextTest.params("http://bob/fred?param1=value1&param2=value2&param1=value3&param3=true"));

    Authenticator rcb = new Authenticator(null, null, null, new LoginDaoMemoryImpl(Duration.ZERO), null, null, true, "X-OpenID-Introspection", false, null, Collections.singletonList("aud"), null);

    Map<String, String> environment = ImmutableMap.<String, String>builder().put("ev1", "good").put("ev2", "bad").build();
    
    RequestContext ctx = new RequestContext(environment
            , UUID.randomUUID().toString()
            , "http://bob/fred?param1=value1&param2=value2&param1=value3&param3=true"
            , "bob"
            , "/fred"
            , RequestContextTest.params("http://bob/fred?param1=value1&param2=value2&param1=value3&param3=true")
            , RequestContextTest.params("X-Forwarded-For=111.122.133.144&X-OpenID-Introspection=" + OPENID)
            , null
            , new IPAddressString("127.0.0.1")
            , new Jwt(null, new JsonObject(new String(Base64.getDecoder().decode(OPENID))), null, null)
    );

    assertFalse(new ConditionInstance("request").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.hasGroup('group1')").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertFalse(new ConditionInstance("request.jwt.hasGroup(\"group7\")").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.getClaimAsList(\"aud\").contains('security-admin-console')").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.audience.contains('security-admin-console')").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.getClaim(\"scope\") =~ '.*[^q]?qe2[^2]?.*'").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.claim[\"scope\"] =~ '.*[^q]?qe2[^2]?.*'").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertFalse(new ConditionInstance("request.jwt.getClaim(\"scope\") =~ '.*[^q]?qe3[^3]?.*'").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertTrue(new ConditionInstance("request.jwt.scope.contains(\"qe2\")").evaluate(ctx, null, DataRow.EMPTY_ROW));
    assertFalse(new ConditionInstance("request.jwt.scope.contains(\"qe1\")").evaluate(ctx, null, DataRow.EMPTY_ROW));

    assertTrue(new ConditionInstance("params.get('param3')").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("params['param3']").evaluate(ctx, null, null));
    assertFalse(new ConditionInstance("params['param4']").evaluate(ctx, null, null));

    assertTrue(new ConditionInstance("'good' == request.env['ev1']").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("'good' == request.env.ev1").evaluate(ctx, null, null));
    assertFalse(new ConditionInstance("'good' == request.env['ev2']").evaluate(ctx, null, null));
    assertFalse(new ConditionInstance("'good' == request.env['ev3']").evaluate(ctx, null, null));
    assertFalse(new ConditionInstance("'good' == request.env.ev3").evaluate(ctx, null, null));
    
    ctx.setJwt(
            new Jwt(null, new JsonObject(new String(Base64.getDecoder().decode(OPENID2))), null, null)
    );

    assertTrue(new ConditionInstance("firstMatchingStringWithPrefix(request.getAudience(), 'client-root-', true) =~ ['bob-sandbox.fred.net', 'bob.fred.net']").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("firstMatchingStringWithPrefix(request.audience, 'client-root-', true) =~ ['bob-sandbox.fred.net', 'bob.fred.net']").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("firstMatchingStringWithPrefix(request.aud, 'client-root-', true) =~ ['bob-sandbox.fred.net', 'bob.fred.net']").evaluate(ctx, null, null));

    assertTrue(new ConditionInstance("allStringsWithPrefix(request.getAudience(), 'client-root-', true).equals(list('bob.fred.net','bob.carol.net'))").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("allStringsWithPrefix(request.audience, 'client-root-', false).equals(list('client-root-bob.fred.net','client-root-bob.carol.net'))").evaluate(ctx, null, null));
    assertTrue(new ConditionInstance("allStringsWithPrefix(request.aud, 'client-root-', true).equals(list('bob.fred.net','bob.carol.net'))").evaluate(ctx, null, null));
    
    lg.setLevel(origLvl);
  }

}
