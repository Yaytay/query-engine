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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.AuditLogMessage.PrimitiveKeyValuePair;

/**
 *
 * @author njt
 */
public class AuditorPersistenceImpl_KvpParserTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // --- parseKeyValuePairs tests ---
  @Test
  void parsesValidArray() {
    String json = """
            [
              {"key":"a","value":"x"},
              {"key":"b","value":123},
              {"key":"c","value":true}
            ]
            """;

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:parsesValidArray", json);

    assertEquals(3, list.size());
    assertEquals("a", list.get(0).getKey());
    assertEquals("x", list.get(0).getValue());
    assertEquals(123, list.get(1).getValue());
    assertEquals(true, list.get(2).getValue());
  }

  @Test
  void returnsEmptyListForNull() {
    assertTrue(AuditorPersistenceImpl.decodeKvp(null, "test:returnsEmptyListForNull", null).isEmpty());
  }

  @Test
  void returnsEmptyListForBlank() {
    assertTrue(AuditorPersistenceImpl.decodeKvp(null, "test:returnsEmptyListForBlank", "   ").isEmpty());
  }

  @Test
  void returnsEmptyListForNonArrayJson() {
    String json = "{\"key\":\"x\"}";
    assertTrue(AuditorPersistenceImpl.decodeKvp(null, "test:returnsEmptyListForNonArrayJson", json).isEmpty());
  }

  @Test
  void skipsEntriesWithMissingKey() {
    String json = """
            [
              {"value":"x"},
              {"key":"ok","value":"y"}
            ]
            """;

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:skipsEntriesWithMissingKey", json);

    assertEquals(1, list.size());
    assertEquals("ok", list.get(0).getKey());
  }

  @Test
  void skipsEntriesWithNonStringKey() {
    String json = """
            [
              {"key":123,"value":"x"},
              {"key":"good","value":"y"}
            ]
            """;

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:skipsEntriesWithNonStringKey", json);

    assertEquals(1, list.size());
    assertEquals("good", list.get(0).getKey());
  }

  @Test
  void handlesBadJsonGracefully() {
    String json = "not valid json";
    assertTrue(AuditorPersistenceImpl.decodeKvp(null, "test:handlesBadJsonGracefully", json).isEmpty());
  }

  @Test
  void handlesNullValue() {
    String json = """
            [
              {"key":"a","value":null}
            ]
            """;

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:handlesNullValue", json);

    assertEquals(1, list.size());
    assertNull(list.get(0).getValue());
  }

  @Test
  void handlesUnexpectedValueTypes() {
    String json = """
            [
              {"key":"a","value":{"nested":1}},
              {"key":"b","value":[1,2,3]}
            ]
            """;

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:handlesUnexpectedValueTypes", json);

    assertEquals(2, list.size());
    assertEquals("{\"nested\":1}", list.get(0).getValue());
    assertEquals("[1,2,3]", list.get(1).getValue());
  }

  @Test
  void constructorTruncatesLongKeys() {
    String longKey = "x".repeat(2000);
    String json = """
            [
              {"key":"%s","value":"v"}
            ]
            """.formatted(longKey);

    List<PrimitiveKeyValuePair> list = AuditorPersistenceImpl.decodeKvp(null, "test:", json);

    assertEquals(999, list.get(0).getKey().length());
  }

  // --- convertJsonValue tests ---
  @Test
  void convertJsonValueHandlesString() throws Exception {
    JsonNode node = MAPPER.readTree("\"hello\"");
    assertEquals("hello", AuditorPersistenceImpl.convertJsonValue(node));
  }

  @Test
  void convertJsonValueHandlesNumber() throws Exception {
    JsonNode node = MAPPER.readTree("42");
    assertEquals(42, AuditorPersistenceImpl.convertJsonValue(node));
  }

  @Test
  void convertJsonValueHandlesBoolean() throws Exception {
    JsonNode node = MAPPER.readTree("true");
    assertEquals(true, AuditorPersistenceImpl.convertJsonValue(node));
  }

  @Test
  void convertJsonValueHandlesNull() throws Exception {
    JsonNode node = MAPPER.readTree("null");
    assertNull(AuditorPersistenceImpl.convertJsonValue(node));
  }

  @Test
  void convertJsonValueHandlesObject() throws Exception {
    JsonNode node = MAPPER.readTree("{\"a\":1}");
    assertEquals("{\"a\":1}", AuditorPersistenceImpl.convertJsonValue(node));
  }

  @Test
  void convertJsonValueHandlesArray() throws Exception {
    JsonNode node = MAPPER.readTree("[1,2,3]");
    assertEquals("[1,2,3]", AuditorPersistenceImpl.convertJsonValue(node));
  }
}
