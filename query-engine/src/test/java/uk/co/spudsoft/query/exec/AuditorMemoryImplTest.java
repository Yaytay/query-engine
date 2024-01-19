/*
 * Copyright (C) 2024 njt
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

import inet.ipaddr.IPAddressString;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import uk.co.spudsoft.query.exec.conditions.RequestContext;


/**
 *
 * @author njt
 */
public class AuditorMemoryImplTest {
  
  @Test
  public void testRowMatches() {
    
    RateLimitRule rule;
    
    AuditorMemoryImpl.AuditRow goodRow = new AuditorMemoryImpl.AuditRow(
            "id", LocalDateTime.MIN, "processId", "url", "127.0.0.1", "host", "path", "arguments", "headers", "openIdDetails", "issuer", "subject", "username", "name", "groups"
    );
    AuditorMemoryImpl.AuditRow badRow = new AuditorMemoryImpl.AuditRow(
            "id", LocalDateTime.MIN, "processId", "url", "127.0.0.2", "host2", "path2", "arguments", "headers", "openIdDetails", "issuer", "subject", "username2", "name", "groups"
    );
    
    Jwt jwt = new Jwt(new JsonObject(), new JsonObject("{\"preferred_username\":\"username\"}"), null, null);
    RequestContext context = new RequestContext("id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), jwt);
    
    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.clientip)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));
    
    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.host)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.path)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.username)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));    
  }
  
  @Test
  public void testNotFound() {
    // Just checking that these don't throw
    RequestContext context = new RequestContext("id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    AuditorMemoryImpl auditor = new AuditorMemoryImpl();
    auditor.recordException(context, new Throwable("test"));
    context = new RequestContext(null, "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    auditor.recordException(context, new Throwable("test"));
  }
  
}
