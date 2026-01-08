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

import uk.co.spudsoft.query.exec.context.RequestContext;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.Status;
import io.vertx.junit5.VertxExtension;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import uk.co.spudsoft.query.exec.Auditor.HistoryFilters;
import uk.co.spudsoft.query.main.OperatorsInstance;

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
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(vertx, null);

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    auditor.recordRequest(context);
    assertTrue(auditor.recordCacheFile(context, "fileName", now).succeeded());
    // This won't do anything, but equally won't barf
    assertTrue(auditor.recordCacheFile(context2, "fileName2", now).succeeded());

    assertTrue(auditor.deleteCacheFile(context, context.getRequestId()).succeeded());
    // This won't do anything, but equally won't barf
    assertTrue(auditor.deleteCacheFile(context2, context2.getRequestId()).succeeded());
  }

  /**
   * Helper to set private fields on AuditRow via reflection.
   */
  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = AuditorMemoryImpl.AuditRow.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  private AuditorMemoryImpl.AuditRow createTestRow() {
    return new AuditorMemoryImpl.AuditRow("id", LocalDateTime.now(), "pid", "url", "ip", "host", "path", "args", "headers");
  }

  // ========================================================================
  // rowMatches Tests
  // ========================================================================
  @Test
  void testRowMatches_NullRequestContext() {
    RateLimitRule rule = RateLimitRule.builder().scope(Collections.singletonList(RateLimitScopeType.host)).build();
    assertThat(AuditorMemoryImpl.rowMatches(null, rule, createTestRow()), is(false));
  }

  @Test
  void testRowMatches_AllScopes() {
    RequestContext ctx = mock(RequestContext.class);
    when(ctx.getClientIp()).thenReturn(new IPAddressString("1.2.3.4"));
    when(ctx.getHost()).thenReturn("host.com");
    when(ctx.getPath()).thenReturn("/path");
    when(ctx.getIssuer()).thenReturn("iss");
    when(ctx.getSubject()).thenReturn("sub");
    when(ctx.getUsername()).thenReturn("user");

    RateLimitScopeType[] allScopes = RateLimitScopeType.values();
    RateLimitRule rule = RateLimitRule.builder().scope(Arrays.asList(allScopes)).build();

    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "clientIp", "1.2.3.4");
    setPrivateField(row, "host", "host.com");
    setPrivateField(row, "path", "/path");
    setPrivateField(row, "issuer", "iss");
    setPrivateField(row, "subject", "sub");
    setPrivateField(row, "username", "user");

    assertThat("Should match when all fields are identical",
            AuditorMemoryImpl.rowMatches(ctx, rule, row), is(true));

    // Test one mismatch (e.g. host)
    when(ctx.getHost()).thenReturn("other.com");
    assertThat("Should fail if even one scope field differs",
            AuditorMemoryImpl.rowMatches(ctx, rule, row), is(false));
  }

  @Test
  void testRowMatches_NullFieldsInContext() {
    RequestContext ctx = mock(RequestContext.class);
    // host is null in context, but present in row
    when(ctx.getHost()).thenReturn(null);
    RateLimitRule rule = RateLimitRule.builder().scope(Collections.singletonList(RateLimitScopeType.host)).build();

    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "host", "some-host");

    assertThat("Context null field should fail match against populated row field",
            AuditorMemoryImpl.rowMatches(ctx, rule, row), is(false));
  }

  // ========================================================================
  // filterHistoryRows Tests
  // ========================================================================
  @Test
  void testFilterHistoryRows_Timestamps() {
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(null, null);
    LocalDateTime rowTime = LocalDateTime.of(2025, 1, 1, 12, 0);
    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "timestamp", rowTime);

    // Start Filter
    assertThat("Row after start should pass",
            auditor.filterHistoryRows(new HistoryFilters(rowTime.minusHours(1), null, null, null, null, null), row), is(true));
    assertThat("Row before start should fail",
            auditor.filterHistoryRows(new HistoryFilters(rowTime.plusHours(1), null, null, null, null, null), row), is(false));

    // End Filter
    assertThat("Row before end should pass",
            auditor.filterHistoryRows(new HistoryFilters(null, rowTime.plusHours(1), null, null, null, null), row), is(true));
    assertThat("Row after end should fail",
            auditor.filterHistoryRows(new HistoryFilters(null, rowTime.minusHours(1), null, null, null, null), row), is(false));
  }

  @Test
  void testFilterHistoryRows_PathPrefix() {
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(null, null);
    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "path", "/api/v1/query");

    assertThat("Prefix match should pass",
            auditor.filterHistoryRows(new HistoryFilters(null, null, null, null, "/api", null), row), is(true));
    assertThat("Mismatching prefix should fail",
            auditor.filterHistoryRows(new HistoryFilters(null, null, null, null, "/web", null), row), is(false));

    setPrivateField(row, "path", null);
    assertThat("Null row path should fail if filter path is set",
            auditor.filterHistoryRows(new HistoryFilters(null, null, null, null, "/api", null), row), is(false));
  }

  @Test
  void testFilterHistoryRows_ResponseCodeExclusion() {
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(null, null);
    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "responseCode", 200);

    // Logic check: if (filterCode != null && filterCode.equals(rowCode)) return false;
    assertThat("Matching code should be excluded (return false)",
            auditor.filterHistoryRows(new HistoryFilters(null, null, null, null, null, 200), row), is(false));
    assertThat("Different code should pass (return true)",
            auditor.filterHistoryRows(new HistoryFilters(null, null, null, null, null, 404), row), is(true));
  }

  @Test
  void testFilterHistoryRows_IssuerAndName() {
    AuditorMemoryImpl auditor = new AuditorMemoryImpl(null, null);
    AuditorMemoryImpl.AuditRow row = createTestRow();
    setPrivateField(row, "issuer", "auth-srv");
    setPrivateField(row, "name", "John");

    assertThat("Matching issuer and name should pass",
            auditor.filterHistoryRows(new HistoryFilters(null, null, "auth-srv", "John", null, null), row), is(true));
    assertThat("Wrong issuer should fail",
            auditor.filterHistoryRows(new HistoryFilters(null, null, "wrong", "John", null, null), row), is(false));
    assertThat("Wrong name should fail",
            auditor.filterHistoryRows(new HistoryFilters(null, null, "auth-srv", "Jane", null, null), row), is(false));
  }

  @Test
  void testCreateComparator() {
    AuditHistoryRow row1 = new AuditHistoryRow(LocalDateTime.now(), "id1", "/path/a", null, "host1", "iss", "sub", "user1", "Name A", 200, 10L, 100L, 50L, 100L, 0);
    AuditHistoryRow row2 = new AuditHistoryRow(LocalDateTime.now().plusMinutes(1), "id2", "/path/b", null, "host2", "iss", "sub", "user2", "Name B", 404, 20L, 200L, 60L, 120L, 0);

    List<AuditHistoryRow> rows = new ArrayList<>(List.of(row1, row2));

    // Test sorting by path ascending
    Comparator<AuditHistoryRow> pathAsc = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.path, false);
    rows.sort(pathAsc);
    assertEquals("/path/a", rows.get(0).getPath());

    // Test sorting by path descending
    Comparator<AuditHistoryRow> pathDesc = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.path, true);
    rows.sort(pathDesc);
    assertEquals("/path/b", rows.get(0).getPath());

    // Test sorting by responseCode ascending
    Comparator<AuditHistoryRow> codeAsc = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseCode, false);
    rows.sort(codeAsc);
    assertEquals(200, rows.get(0).getResponseCode());

    // Test sorting by responseCode descending
    Comparator<AuditHistoryRow> codeDesc = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.responseCode, true);
    rows.sort(codeDesc);
    assertEquals(404, rows.get(0).getResponseCode());
  }

  @Test
  void testCreateComparatorExhaustive() {
    LocalDateTime now = LocalDateTime.now();
    
    // Create two rows where row2 is "greater" than row1 in every sortable field
    AuditHistoryRow row1 = new AuditHistoryRow(
        now, "id1", "/path/a", null, "host1", "iss1", "sub1", "user1", "Name A", 200, 10L, 100L, 50L, 1000L, 0);
    
    AuditHistoryRow row2 = new AuditHistoryRow(
        now.plusMinutes(1), "id2", "/path/b", null, "host2", "iss2", "sub2", "user2", "Name B", 404, 20L, 200L, 60L, 2000L, 5);

    List<AuditHistoryRow> rows = new ArrayList<>();

    for (AuditHistorySortOrder order : AuditHistorySortOrder.values()) {
      // Test Ascending: row1 should come first
      rows.clear();
      rows.addAll(List.of(row2, row1));
      Comparator<AuditHistoryRow> asc = AuditorMemoryImpl.createComparator(order, false);
      rows.sort(asc);
      
      assertEquals(row1.getId(), rows.get(0).getId(), "Failed ascending sort for " + order);

      // Test Descending: row2 should come first
      rows.clear();
      rows.addAll(List.of(row1, row2));
      Comparator<AuditHistoryRow> desc = AuditorMemoryImpl.createComparator(order, true);
      rows.sort(desc);
      
      assertEquals(row2.getId(), rows.get(0).getId(), "Failed descending sort for " + order);
    }
  }
  
  @Test
  void testComparatorNullHandling() {
    AuditHistoryRow rowWithNull = new AuditHistoryRow(LocalDateTime.now(), "id1", null, null, null, null, null, null, null, null, null, null, null, null, 0);
    AuditHistoryRow rowWithVal = new AuditHistoryRow(LocalDateTime.now(), "id2", "path", null, "host", "iss", "sub", "user", "name", 200, 10L, 100L, 50L, 100L, 0);

    List<AuditHistoryRow> rows = new ArrayList<>(List.of(rowWithVal, rowWithNull));

    // buildComparator uses nullsLast
    Comparator<AuditHistoryRow> pathAsc = AuditorMemoryImpl.createComparator(AuditHistorySortOrder.path, false);
    rows.sort(pathAsc);

    assertEquals("path", rows.get(0).getPath());
    assertEquals(null, rows.get(1).getPath());
  }
  
  @Test
  void testHealthCheck(Vertx vertx) {
    
    AuditorMemoryImpl impl = new AuditorMemoryImpl(vertx, new OperatorsInstance(null));
    Promise<Status> promise = Promise.promise();
    impl.healthCheck(promise);
    assertTrue(promise.future().isComplete());
    assertTrue(promise.future().result().isOk());
  }
}
