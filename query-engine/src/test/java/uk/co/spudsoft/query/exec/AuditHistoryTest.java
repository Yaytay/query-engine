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

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class AuditHistoryTest {

  @Test
  public void testGetTimestamp() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(LocalDateTime.MAX, ah.getTimestamp());
  }

  @Test
  public void testGetId() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("id", ah.getId());
  }

  @Test
  public void testGetPath() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("path", ah.getPath());
  }

  @Test
  public void testGetArguments() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertNotNull(ah.getArguments());
    assertEquals(1, ah.getArguments().size());
    assertEquals("value", ah.getArguments().getString("arg1"));
  }

  @Test
  public void testGetHost() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("host", ah.getHost());
  }

  @Test
  public void testGetIssuer() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("issuer", ah.getIssuer());
  }

  @Test
  public void testGetSubject() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("subject", ah.getSubject());
  }

  @Test
  public void testGetUsername() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("username", ah.getUsername());
  }

  @Test
  public void testGetName() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals("Full Name", ah.getName());
  }

  @Test
  public void testGetResponseCode() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(200, ah.getResponseCode());
  }

  @Test
  public void testGetResponseRows() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(1, ah.getResponseRows());
  }

  @Test
  public void testGetResponseSize() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(2, ah.getResponseSize());
  }

  @Test
  public void testGetResponseStreamStart() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(3, ah.getResponseStreamStart().toMillis());
  }

  @Test
  public void testGetResponseDuration() {
    AuditHistory ah = new AuditHistory(LocalDateTime.MAX, "id", "path", new JsonObject("{\"arg1\": \"value\"}"), "host", "issuer", "subject", "username", "Full Name", 200, 1, 2, 3, 4);
    assertEquals(4, ah.getResponseDuration().toMillis());
  }

}
