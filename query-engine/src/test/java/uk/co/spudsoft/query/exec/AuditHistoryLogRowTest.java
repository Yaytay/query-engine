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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AuditHistoryLogRowTest {

  @Test
  void testConstructorAndGetters() {
    LocalDateTime now = LocalDateTime.now();
    List<AuditLogMessage.PrimitiveKeyValuePair> kvp = Collections.emptyList();

    AuditHistoryLogRow row = new AuditHistoryLogRow(
      now, "pipe1", "INFO", "LoggerName", "Thread-1", "Test message", kvp
    );

    assertEquals(now, row.getTimestamp());
    assertEquals("pipe1", row.getPipe());
    assertEquals("INFO", row.getLevel());
    assertEquals("LoggerName", row.getLoggerName());
    assertEquals("Thread-1", row.getThreadName());
    assertEquals("Test message", row.getMessage());
    assertEquals(kvp, row.getKvp());
  }
}
