/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import uk.co.spudsoft.query.exec.AuditLogMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequestCollatingAppender}.
 */
public class RequestCollatingAppenderTest {

  private LoggerContext loggerContext;
  private RequestCollatingAppender appender;

  @BeforeEach
  void setUp() {
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    appender = new RequestCollatingAppender();
    appender.setContext(loggerContext);
    appender.append(null);
    appender.clearAll();
    appender.purgeOlderThanDefault();
    appender.start();
    appender.append(null);
  }

  @AfterEach
  void tearDown() {
    appender.stop();
  }

  /**
   * Helper to create a LoggingEvent with given parameters.
   */
  private ILoggingEvent createEvent(
      Level level,
      String message,
      String requestId,
      String pipe,
      long timestampMillis
  ) {
    ch.qos.logback.classic.Logger logger =
        loggerContext.getLogger("test.logger");

    LoggingEvent event = new LoggingEvent(
        RequestCollatingAppenderTest.class.getName(),
        logger,
        level,
        message,
        null,
        null
    );
    event.setTimeStamp(timestampMillis);

    // Add KeyValuePair entries for requestId/pipe if present
    java.util.List<KeyValuePair> kvps = new java.util.ArrayList<>();
    if (requestId != null) {
      kvps.add(new KeyValuePair(Log.REQUEST_ID_KEY, requestId));
    }
    if (pipe != null) {
      kvps.add(new KeyValuePair(Log.PIPE_KEY, pipe));
    }
    event.setKeyValuePairs(kvps);

    return event;
  }

  @Test
  void recordsEventsByRequestIdAndReturnsInOrder() {
    String requestId = "req-123";
    long baseTs = System.currentTimeMillis();

    ILoggingEvent e1 = createEvent(Level.WARN, "message-1", requestId, "pipeA", baseTs);
    ILoggingEvent e2 = createEvent(Level.WARN, "message-2", requestId, "pipeA", baseTs + 10);

    appender.doAppend(e1);
    appender.doAppend(e2);

    Map<String, List<AuditLogMessage>> snapshot = appender.snapshotAllEvents();
    assertEquals(1, snapshot.size(), "Should have one requestId entry");
    assertTrue(snapshot.containsKey(requestId));

    List<AuditLogMessage> stored = snapshot.get(requestId);
    assertEquals(2, stored.size(), "Should have recorded two events");
    assertEquals("message-1", stored.get(0).getMessage());
    assertEquals("message-2", stored.get(1).getMessage());
    assertEquals("pipeA", stored.get(0).getPipe());

    // getAndRemoveEventsForRequest should return and clear
    List<AuditLogMessage> retrieved = appender.getAndRemoveEventsForRequest(requestId);
    assertEquals(2, retrieved.size());
    assertEquals("message-1", retrieved.get(0).getMessage());
    assertEquals("message-2", retrieved.get(1).getMessage());

    // After removal, snapshot should be empty
    assertTrue(appender.snapshotAllEvents().isEmpty());
  }

  @Test
  void doesNotRecordEventsWithoutRequestId() {
    long ts = System.currentTimeMillis();
    ILoggingEvent e1 = createEvent(Level.INFO, "no-request-id", null, "pipeA", ts);

    appender.doAppend(e1);

    assertTrue(appender.snapshotAllEvents().isEmpty(), "Events without requestId should be ignored");
  }

  @Test
  void respectsMinimumLevel() {
    appender.setMinimumLevel("WARN");
    // This shouldn't change anything
    appender.setMinimumLevel(null);
    // This also shouldn't change anything
    appender.setMinimumLevel("wibble");

    String requestId = "req-lev";
    long ts = System.currentTimeMillis();

    ILoggingEvent debugEvent = createEvent(Level.DEBUG, "debug-msg", requestId, "pipe", ts);
    ILoggingEvent infoEvent = createEvent(Level.INFO, "info-msg", requestId, "pipe", ts + 1);
    ILoggingEvent warnEvent = createEvent(Level.WARN, "warn-msg", requestId, "pipe", ts + 2);

    appender.doAppend(debugEvent);
    appender.doAppend(infoEvent);
    appender.doAppend(warnEvent);

    Map<String, List<AuditLogMessage>> snapshot = appender.snapshotAllEvents();
    assertEquals(1, snapshot.size());
    List<AuditLogMessage> stored = snapshot.get(requestId);

    // Only WARN and above should be stored
    assertEquals(1, stored.size());
    assertEquals("warn-msg", stored.get(0).getMessage());
  }

  @Test
  void enforcesMaxEventsPerRequest() {
    appender.setMaxEventsPerRequest(2);
    // This shouldn't do anything
    appender.setMaxEventsPerRequest(0);
    
    appender.setMinimumLevel("INFO");

    String requestId = "req-cap";
    long ts = System.currentTimeMillis();

    ILoggingEvent e1 = createEvent(Level.INFO, "msg-1", requestId, null, ts);
    ILoggingEvent e2 = createEvent(Level.INFO, "msg-2", requestId, null, ts + 1);
    ILoggingEvent e3 = createEvent(Level.INFO, "msg-3", requestId, null, ts + 2);

    appender.doAppend(e1);
    appender.doAppend(e2);
    appender.doAppend(e3);

    List<AuditLogMessage> stored = appender.snapshotAllEvents().get(requestId);
    assertNotNull(stored);
    assertEquals(2, stored.size(), "Should only keep first 2 events");
    assertEquals("msg-1", stored.get(0).getMessage());
    assertEquals("msg-2", stored.get(1).getMessage());
  }

  @Test
  void getAndRemoveEventsForRequestHandlesMissingId() {
    assertTrue(appender.getAndRemoveEventsForRequest(null).isEmpty());
    assertTrue(appender.getAndRemoveEventsForRequest("unknown").isEmpty());
  }

  @Test
  void purgeOlderThanRemovesOldEntriesOnly() {
    String oldReq = "old-req";
    String newReq = "new-req";

    // Old events: 2 hours ago
    LocalDateTime twoHoursAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(2);
    long oldTs = twoHoursAgo.toInstant(ZoneOffset.UTC).toEpochMilli();

    // New events: now
    long newTs = System.currentTimeMillis();

    ILoggingEvent oldEvent = createEvent(Level.WARN, "old", oldReq, null, oldTs);
    ILoggingEvent newEvent = createEvent(Level.WARN, "new", newReq, null, newTs);

    appender.doAppend(oldEvent);
    appender.doAppend(newEvent);

    assertEquals(2, appender.snapshotAllEvents().size(), "Both entries should be present before purge");

    // Purge anything older than 1 hour
    appender.purgeOlderThan(Duration.ofHours(1));

    Map<String, List<AuditLogMessage>> snapshot = appender.snapshotAllEvents();
    assertEquals(1, snapshot.size(), "Only one entry should remain after purge");
    assertFalse(snapshot.containsKey(oldReq), "Old request should have been purged");
    assertTrue(snapshot.containsKey(newReq), "New request should remain");

    // No errors for sending in garbage
    appender.purgeOlderThan(null);
    appender.purgeOlderThan(Duration.ofDays(-1));
    appender.purgeOlderThan(Duration.ZERO);

  }

  @Test
  void getKvpStringValueReturnsMatchingStringOrNull() {
    java.util.List<KeyValuePair> kvps = java.util.List.of(
        new KeyValuePair("k1", "v1"),
        new KeyValuePair("k2", 123),           // non-string
        new KeyValuePair("k3", "v3")
    );

    assertEquals("v1", RequestCollatingAppender.getKvpStringValue(kvps, "k1"));
    assertNull(RequestCollatingAppender.getKvpStringValue(kvps, "k2"),
        "Non-string value should return null");
    assertEquals("v3", RequestCollatingAppender.getKvpStringValue(kvps, "k3"));
    assertNull(RequestCollatingAppender.getKvpStringValue(kvps, "missing"));
  }

  @Test
  void fromBuildsAuditLogMessageCorrectly() {
    String requestId = "req-from";
    String pipe = "pipe-X";
    long ts = System.currentTimeMillis();

    ILoggingEvent event = createEvent(Level.INFO, "hello-from", requestId, pipe, ts);

    AuditLogMessage msg = RequestCollatingAppender.from(event);
    assertNotNull(msg);
    assertEquals("pipe-X", msg.getPipe());
    assertEquals("hello-from", msg.getMessage());
    assertEquals("INFO", msg.getLevel());
    assertNotNull(msg.getTimestamp());
    assertNotNull(msg.getLoggerName());
    assertNotNull(msg.getThreadName());
    assertNotNull(msg.getKvpData(), "KVP JSON should be populated");
  }

}