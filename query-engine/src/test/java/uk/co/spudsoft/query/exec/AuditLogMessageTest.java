/*
 * Copyright (C) 2026 njt
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

/**
 * Unit tests for {@link AuditLogMessage}.
 * @author njt
 */
public class AuditLogMessageTest {

  @Test
  public void testPrimitiveKeyValuePairSanitization() {
    // Test primitive types (allowed as-is)
    assertEquals("string", new AuditLogMessage.PrimitiveKeyValuePair("k", "string").getValue());
    assertEquals(123, new AuditLogMessage.PrimitiveKeyValuePair("k", 123).getValue());
    assertEquals(true, new AuditLogMessage.PrimitiveKeyValuePair("k", true).getValue());
    assertEquals(null, new AuditLogMessage.PrimitiveKeyValuePair("k", null).getValue());

    // Test non-primitive type (should be converted to String)
    Object complexObj = new Object() {
      @Override
      public String toString() {
        return "complex";
      }
    };
    assertEquals("complex", new AuditLogMessage.PrimitiveKeyValuePair("k", complexObj).getValue());
  }

  @Test
  public void testPrimitiveKeyValuePairKeyTruncation() {
    String longKey = "a".repeat(1100);
    AuditLogMessage.PrimitiveKeyValuePair kvp = new AuditLogMessage.PrimitiveKeyValuePair(longKey, "val");

    assertEquals(999, kvp.getKey().length());
    assertTrue(longKey.startsWith(kvp.getKey()));
  }

  @Test
  public void testAuditLogMessageConstructor() {
    LocalDateTime now = LocalDateTime.now();
    List<KeyValuePair> slf4jKvps = Arrays.asList(
      new KeyValuePair("key1", "val1"),
      new KeyValuePair("key2", 456)
    );

    AuditLogMessage message = new AuditLogMessage(
      "myPipe", now, "ERROR", "com.test.Logger", "main-thread", slf4jKvps, "Something went wrong"
    );

    assertEquals("myPipe", message.getPipe());
    assertEquals(now, message.getTimestamp());
    assertEquals("ERROR", message.getLevel());
    assertEquals("com.test.Logger", message.getLoggerName());
    assertEquals("main-thread", message.getThreadName());
    assertEquals("Something went wrong", message.getMessage());

    assertNotNull(message.getKvpData());
    assertEquals(2, message.getKvpData().size());
    assertEquals("key1", message.getKvpData().get(0).getKey());
    assertEquals("val1", message.getKvpData().get(0).getValue());
    assertEquals("key2", message.getKvpData().get(1).getKey());
    assertEquals(456, message.getKvpData().get(1).getValue());
  }

  @Test
  public void testAuditLogMessageWithNullKvps() {
    AuditLogMessage message = new AuditLogMessage(
      "p", LocalDateTime.now(), "INFO", "L", "T", null, "msg"
    );

    assertNotNull(message.getKvpData());
    assertTrue(message.getKvpData().isEmpty());
  }
}
