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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.google.common.base.Strings;
import io.vertx.core.json.Json;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import uk.co.spudsoft.query.exec.AuditLogMessage;

/**
 * Logback appender that collates log events per request into an in-memory buffer.
 * <p>
 * Each event is grouped by a Log.REQUEST_ID_KEY entry in the KeyValuePair data. 
 * 
 * The internal storage is:
 *
 *   Map&lt;String, List&lt;AuditLogMessage&gt;&gt;
 *
 * where the key is the requestId and the value is the ordered list of processed log events.
 *
 * This appender does NOT write to a database and does NOT interfere with other appenders.
 * You are expected to:
 *   - Add this appender alongside your normal console/file appenders.
 *   - After a request completes, call {@link #getAndRemoveEventsForRequest(String)}
 *     to obtain all events, serialize them (e.g. to JSON), and write them to your DB.
 *   - Periodically call {@link #purgeOlderThanDefault()} or {@link #purgeOlderThan(Duration)}
 *     to avoid keeping old, unused entries forever.
 */
public class RequestCollatingAppender extends AppenderBase<ILoggingEvent> {
  
  private static final Logger logger = LoggerFactory.getLogger(RequestCollatingAppender.class);

  /**
   * Constructor.
   */
  public RequestCollatingAppender() {
  }

  /**
   * In-memory store:
   *   requestId -> list of events for that request (in arrival order).
   */
  private final Map<String, List<AuditLogMessage>> eventsByRequestId = new ConcurrentHashMap<>();

  /**
   * Optional: minimum level of events to record (defaults to INFO).
   */
  private Level minimumLevel = Level.WARN;

  /**
   * Optional: maximum number of events per request (to avoid unbounded memory use).
   * Only the first maxEventsPerRequest events for a given requestId are recorded.
   */
  private int maxEventsPerRequest = 100;

  /**
   * Default retention duration for purgeOlderThanDefault(): 12 hours.
   */
  private static final Duration DEFAULT_RETENTION = Duration.ofHours(12);

  static String getKvpStringValue(List<KeyValuePair> kvps, String key) {
    if (kvps != null) {
      for (KeyValuePair kvp : kvps) {
        if (key.equals(kvp.key)) {
          if (kvp.value instanceof String s) {
            return s;
          }
        }
      }
    }
    return null;
  }
  
  @Override
  protected void append(ILoggingEvent event) {
    if (!isStarted() || event == null) {
      return;
    }

    // Filter by level
    if (!event.getLevel().isGreaterOrEqual(minimumLevel)) {
      return;
    }

    // Extract requestId from MDC
    String requestId = getKvpStringValue(event.getKeyValuePairs(), Log.REQUEST_ID_KEY);
    if (Strings.isNullOrEmpty(requestId)) {
      // No requestId present; ignore for this appender
      return;
    }

    // Build processed representation
    AuditLogMessage processed = from(event);

    // Store in per-request list; only keep the first maxEventsPerRequest events
    eventsByRequestId.compute(requestId, (key, existing) -> {
      if (existing == null) {
        existing = Collections.synchronizedList(new ArrayList<>());
      }
      if (existing.size() < maxEventsPerRequest) {
        existing.add(processed);
      }
      return existing;
    });
  }

  static AuditLogMessage from(ILoggingEvent event) {
    String pipe = getKvpStringValue(event.getKeyValuePairs(), Log.PIPE_KEY);
    LocalDateTime ts = Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneOffset.UTC).toLocalDateTime();
    String level = event.getLevel() != null ? event.getLevel().toString() : null;
    String logger = event.getLoggerName();
    String thread = event.getThreadName();
    String msg = event.getFormattedMessage();
    IThrowableProxy exception = event.getThrowableProxy();
    if (exception != null) {
      msg = msg + exception.getClassName() + ": " + exception.getMessage();
    }
    String kvpJson = null;
    try {
      kvpJson = Json.encode(event.getKeyValuePairs());
    } catch (Throwable ex) {
      // Ignore error
    }
    return new AuditLogMessage(pipe, ts, level, logger, thread, kvpJson, msg);
  }
  
  /**
   * Retrieve and remove all events associated with the given requestId.
   * <p>
   * This is the main entry point for your DB writer: call this after a request completes,
   * then write the returned events in a single DB transaction.
   *
   * @param requestId the request ID whose events should be fetched.
   * @return the list of events for this request, in order of arrival, or an empty list
   * if there were none.
   */
  public List<AuditLogMessage> getAndRemoveEventsForRequest(String requestId) {
    if (requestId == null) {
      return List.of();
    }
    List<AuditLogMessage> list = eventsByRequestId.remove(requestId);
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    // Return an immutable copy to avoid external modification of our internal list
    synchronized (list) {
      return List.copyOf(list);
    }
  }

  /**
   * Get a snapshot of all stored events (for debugging/introspection).
   * <p>
   * This returns a deep, read-only copy of the storage at the time of the call.
   * Modifying the returned map or lists will not affect the appender's internal state.
   * @return a copy of the currently held data.
   */
  public Map<String, List<AuditLogMessage>> snapshotAllEvents() {
    Map<String, List<AuditLogMessage>> copy = new HashMap<>();
    for (Map.Entry<String, List<AuditLogMessage>> entry : eventsByRequestId.entrySet()) {
      List<AuditLogMessage> list = entry.getValue();
      synchronized (list) {
        copy.put(entry.getKey(), List.copyOf(list));
      }
    }
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Clear all stored events for all requestIds.
   */
  public void clearAll() {
    eventsByRequestId.clear();
  }

  /**
   * Purge any request entries whose events are all older than the default retention period
   * (12 hours) compared to {@link Instant#now()}.
   * <p>
   * Intended to be called periodically (e.g. from a Vert.x timer) to ensure we don't
   * keep events forever if getAndRemoveEventsForRequest is never called.
   */
  public void purgeOlderThanDefault() {
    purgeOlderThan(DEFAULT_RETENTION);
  }

  /**
   * Purge any request entries whose events are all older than the specified retention
   * duration compared to {@link Instant#now()}.
   * <p>
   * Logic:
   * - For each requestId, find the most recent event timestamp.
   * - If the most recent event is still older than now - retention, the entire entry
   *   is removed.
   *
   * @param retention the retention duration; entries older than now - retention are removed.
   */
  public void purgeOlderThan(Duration retention) {
    if (retention == null || retention.isNegative() || retention.isZero()) {
      return;
    }
    LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minus(retention);
    logger.debug("Purging log records older than {}", cutoff);

    for (Map.Entry<String, List<AuditLogMessage>> entry : eventsByRequestId.entrySet()) {
      String requestId = entry.getKey();
      List<AuditLogMessage> list = entry.getValue();
      if (list == null || list.isEmpty()) {
        eventsByRequestId.remove(requestId);
        continue;
      }

      LocalDateTime newest = null;
      synchronized (list) {
        try {
          for (AuditLogMessage e : list) {
            LocalDateTime  t = e.getTimestamp();
            if (newest == null || t.isAfter(newest)) {
              newest = t;
            }
          }
        } catch (Throwable ex) {
          logger.warn("Error finding latest log record for {}: ", requestId, ex);
          newest = null;
        }
      }

      if (newest == null || newest.isBefore(cutoff)) {
        // Entire entry is older than the cutoff; remove it.
        eventsByRequestId.remove(requestId);
      }
    }
  }

  // ---- Configuration setters (usable from logback.xml via reflection) ----

  /**
   * Set the minimum level of events that should be recorded.
   * Accepts standard Logback level names (e.g. "DEBUG", "INFO", "WARN", "ERROR").
   *
   * @param levelName the minimum level as a string.
   */
  public void setMinimumLevel(String levelName) {
    if (levelName == null) {
      return;
    }
    Level parsed = Level.toLevel(levelName.trim(), null);
    if (parsed != null) {
      this.minimumLevel = parsed;
    }
  }

  /**
   * Set the maximum number of events that will be stored per request.
   * Only the first maxEventsPerRequest events are recorded; additional events
   * for the same requestId are ignored.
   *
   * If the provided value is less than 1, the default of 100 is kept.
   * 
   * @param maxEventsPerRequest The maximum number of event log messages to record per requestId.
   */
  public void setMaxEventsPerRequest(int maxEventsPerRequest) {
    if (maxEventsPerRequest >= 1) {
      this.maxEventsPerRequest = maxEventsPerRequest;
    }
  }

}
