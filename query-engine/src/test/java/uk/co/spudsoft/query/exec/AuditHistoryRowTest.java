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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AuditHistoryRowTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void testConstructorAndBasicGetters() {
    LocalDateTime now = LocalDateTime.now();
    ObjectNode args = mapper.createObjectNode();

    AuditHistoryRow row = new AuditHistoryRow(
      now, "id1", "path/to/pipe", args, "host1", "issuer1", "sub1", "user1", "Name",
      200, 100L, 1024L, 500L, 1500L, 2
    );

    assertEquals(now, row.getTimestamp());
    assertEquals("id1", row.getId());
    assertEquals("path/to/pipe", row.getPath());
    assertEquals(args, row.getArguments());
    assertEquals("host1", row.getHost());
    assertEquals("issuer1", row.getIssuer());
    assertEquals("sub1", row.getSubject());
    assertEquals("user1", row.getUsername());
    assertEquals("Name", row.getName());
    assertEquals(200, row.getResponseCode());
    assertEquals(100L, row.getResponseRows());
    assertEquals(1024L, row.getResponseSize());
    assertEquals(2, row.getWarningCount());

    // Verify ms to seconds conversion
    assertEquals(0.5, row.getResponseStreamStart());
    assertEquals(1.5, row.getResponseDuration());
  }

  @Test
  void testTimeConversionWithNulls() {
    AuditHistoryRow row = new AuditHistoryRow(
      LocalDateTime.now(), "id", "path", null, "host", "issuer", "sub", "user", "name",
      200, 0L, 0L, null, null, 0
    );

    assertNull(row.getResponseStreamStart());
    assertNull(row.getResponseDuration());
  }

  @Test
  void testWarningsAndSources() {
    AuditHistoryRow row = new AuditHistoryRow(
      LocalDateTime.now(), "id", "path", null, "host", "issuer", "sub", "user", "name",
      200, 0L, 0L, 0L, 0L, 0
    );

    assertNull(row.getWarnings());
    assertNull(row.getSources());

    List<AuditHistoryLogRow> warnings = Collections.singletonList(
      new AuditHistoryLogRow(LocalDateTime.now(), "p", "WARN", "L", "T", "msg", null)
    );
    List<AuditHistorySourceRow> sources = Collections.singletonList(
      new AuditHistorySourceRow("p", LocalDateTime.now(), "h", "e", "u", "un", "q", null)
    );

    row.setWarnings(warnings);
    row.setSources(sources);

    assertEquals(1, row.getWarnings().size());
    assertEquals(1, row.getSources().size());
    assertEquals("msg", row.getWarnings().get(0).getMessage());
    assertEquals("h", row.getSources().get(0).getSourceHash());
  }
}
