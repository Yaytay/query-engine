/*
 * Copyright (C) 2024 jtalbut
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
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import uk.co.spudsoft.query.exec.conditions.RequestContext;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class AuditorMemoryImplTest {
  
  @Test
  public void testDeleteCacheFile(Vertx vertx) {
    RequestContext context = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    RequestContext context2 = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(vertx);
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    auditor.recordRequest(context);
    assertTrue(auditor.recordCacheFile(context, "fileName", now).succeeded());
    // This won't do anything, but equally won't barf
    assertTrue(auditor.recordCacheFile(context2, "fileName2", now).succeeded());
    
    assertTrue(auditor.deleteCacheFile(context.getRequestId()).succeeded());
    // This won't do anything, but equally won't barf
    assertTrue(auditor.deleteCacheFile(context2.getRequestId()).succeeded());
    
  }
  
  @Test
  public void testRowMatches() {
    
    RateLimitRule rule;
    
    AuditorMemoryImpl.AuditRow goodRow = new AuditorMemoryImpl.AuditRow(
            "id", LocalDateTime.MIN, "processId", "url", "127.0.0.1", "host", "path", "arguments", "headers", "openIdDetails", "issuer", "subject", "username", "name", "groups"
    );
    AuditorMemoryImpl.AuditRow badRow = new AuditorMemoryImpl.AuditRow(
            "id", LocalDateTime.MIN, "processId", "url", "127.0.0.2", "host2", "path2", "arguments", "headers", "openIdDetails", "issuer2", "subject2", "username2", "name", "groups"
    );
    AuditorMemoryImpl.AuditRow nullRow = new AuditorMemoryImpl.AuditRow(
            "id", LocalDateTime.MIN, "processId", "url", null, null, null, null, "headers", "openIdDetails", null, null, null, "name", "groups"
    );
    
    Jwt jwt = new Jwt(new JsonObject(), new JsonObject("{\"iss\":\"issuer\",\"sub\":\"subject\",\"preferred_username\":\"username\"}"), null, null);
    RequestContext context = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), jwt);
    
    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.clientip)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));
    
    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.host)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.path)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.issuer)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));    
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.subject)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));    
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));

    rule = RateLimitRule.builder().scope(Arrays.asList(RateLimitScopeType.username)).build();
    assertTrue(AuditorMemoryImpl.rowMatches(context, rule, goodRow));
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, badRow));    
    assertFalse(AuditorMemoryImpl.rowMatches(context, rule, nullRow));
  }
  
  @Test
  public void testNotFound(Vertx vertx) {
    // Just checking that these don't throw
    RequestContext context = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(vertx);
    auditor.recordException(context, new Throwable("test"));
    context = new RequestContext(null, null, "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    auditor.recordException(context, new Throwable("test"));
  }

  @Test
  public void testCreateComparatorId() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, "A", null, null, null, null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, "B", null, null, null, null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.id, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.id, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorPath() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, "A", null, null, null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, "B", null, null, null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.path, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.path, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorHost() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, "A", null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, "B", null, null, null, null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.host, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.host, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorIssuer() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, "A", null, null, null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, "B", null, null, null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.issuer, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.issuer, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorSubject() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, "A", null, null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, "B", null, null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.subject, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.subject, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorUsername() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, "A", null, null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, "B", null, null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.username, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.username, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorName() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, "A", null, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, "B", null, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.name, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.name, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorResponseCode() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, 200, null, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, 400, null, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseCode, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseCode, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorResponseRows() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, 7L, null, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, 8L, null, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseRows, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseRows, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorResponseSize() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, 8L, null, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, 9L, null, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseSize, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseSize, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorResponseStreamStart() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, 3456L, null);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, 4567L, null);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseStreamStart, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseStreamStart, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
  @Test
  public void testCreateComparatorResponseDuration() {
    Comparator<AuditHistoryRow> comparator;
    AuditHistoryRow row1 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, 1234L);
    AuditHistoryRow row2 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, 2345L);
    AuditHistoryRow row3 = new AuditHistoryRow(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseDuration, false);
    assertThat(comparator.compare(row1, row2), lessThan(0));
    assertThat(comparator.compare(row1, row3), lessThan(0));
    assertThat(comparator.compare(row2, row1), greaterThan(0));
    assertThat(comparator.compare(row2, row3), lessThan(0));
    assertThat(comparator.compare(row3, row1), greaterThan(0));
    assertThat(comparator.compare(row3, row2), greaterThan(0));
    comparator = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseDuration, true);
    assertThat(comparator.compare(row1, row2), greaterThan(0));
    assertThat(comparator.compare(row1, row3), greaterThan(0));
    assertThat(comparator.compare(row2, row1), lessThan(0));
    assertThat(comparator.compare(row2, row3), greaterThan(0));
    assertThat(comparator.compare(row3, row1), lessThan(0));
    assertThat(comparator.compare(row3, row2), lessThan(0));
  }
  
}
