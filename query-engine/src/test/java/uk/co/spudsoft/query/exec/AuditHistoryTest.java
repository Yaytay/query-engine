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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.jackson.DatabindCodec;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class AuditHistoryTest {
  
  private ObjectMapper mapper = DatabindCodec.mapper();

  @Test
  public void testGetTimestamp() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(LocalDateTime.MAX, ah.getTimestamp());
  }

  @Test
  public void testGetId() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("id", ah.getId());
  }

  @Test
  public void testGetPath() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("path", ah.getPath());
  }

  @Test
  public void testGetArguments() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertNotNull(ah.getArguments());
    assertEquals(1, ah.getArguments().size());
    assertEquals("value", ah.getArguments().get("arg1").textValue());
  }

  @Test
  public void testGetHost() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("host", ah.getHost());
  }

  @Test
  public void testGetIssuer() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("issuer", ah.getIssuer());
  }

  @Test
  public void testGetSubject() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("subject", ah.getSubject());
  }

  @Test
  public void testGetUsername() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("username", ah.getUsername());
  }

  @Test
  public void testGetName() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals("Full Name", ah.getName());
  }

  @Test
  public void testGetResponseCode() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(200, ah.getResponseCode());
  }

  @Test
  public void testGetResponseRows() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(1, ah.getResponseRows());
  }

  @Test
  public void testGetResponseSize() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(2, ah.getResponseSize());
  }

  @Test
  public void testGetResponseStreamStart() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(0.003, ah.getResponseStreamStart());
  }

  @Test
  public void testGetResponseDuration() throws JsonProcessingException {
    AuditHistoryRow ah = new AuditHistoryRow(LocalDateTime.MAX, "id", "path", mapper.readValue("{\"arg1\": \"value\"}", ObjectNode.class), "host", "issuer", "subject", "username", "Full Name", 200, 1L, 2L, 3L, 4L);
    assertEquals(0.004, ah.getResponseDuration());
  }

}
