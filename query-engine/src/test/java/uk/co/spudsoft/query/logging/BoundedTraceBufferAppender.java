package uk.co.spudsoft.query.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * Appender that buffers the first N WARN+ logs per traceId while collection is active.
 * Once a trace's capacity is reached, further events for that trace are ignored.
 */
public class BoundedTraceBufferAppender extends AppenderBase<ILoggingEvent> {

  // Configurable
  private String mdcKey = "traceId";
  private Level minimumLevel = Level.WARN;
  private int perTraceCapacity = 200;
  private int maxActiveTraces = 1000;

  private Supplier<String> traceIdProvider;

  // Active traces we should buffer for
  private final Set<String> activeTraces = ConcurrentHashMap.newKeySet();
  // Buffers per traceId
  private final ConcurrentHashMap<String, FirstNBuffer<BufferedLog>> buffers = new ConcurrentHashMap<>();
  private final Set<String> bufferedTraceIds = ConcurrentHashMap.newKeySet();

  public void setMdcKey(String mdcKey) {
    this.mdcKey = Objects.requireNonNull(mdcKey);
  }

  public void setMinimumLevel(String level) {
    this.minimumLevel = Level.toLevel(level, Level.WARN);
  }

  public void setPerTraceCapacity(int perTraceCapacity) {
    this.perTraceCapacity = Math.max(1, perTraceCapacity);
  }

  public void setMaxActiveTraces(int maxActiveTraces) {
    this.maxActiveTraces = Math.max(10, maxActiveTraces);
  }

  public void setTraceIdProvider(Supplier<String> traceIdProvider) {
    this.traceIdProvider = traceIdProvider;
  }

  // Control API

  public boolean startCollecting(String traceId) {
    if (!isStarted()) return false;
    if (traceId == null || traceId.isBlank()) return false;
    activeTraces.add(traceId);
    return true;
  }

  public boolean stopCollecting(String traceId) {
    if (traceId == null) return false;
    activeTraces.remove(traceId);
    FirstNBuffer<BufferedLog> buf = buffers.remove(traceId);
    if (buf != null) {
      bufferedTraceIds.remove(traceId);
      return true;
    }
    return false;
  }

  /**
   * Returns a snapshot copy of buffered logs for the traceId without clearing the buffer.
   * Call stopCollecting(traceId) to free memory when done.
   */
  public Collection<BufferedLog> getLogs(String traceId) {
    if (traceId == null) return Collections.emptyList();
    FirstNBuffer<BufferedLog> buf = buffers.get(traceId);
    if (buf == null) return Collections.emptyList();
    return buf.snapshot();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (!isStarted() || event == null) return;

    if (event.getLevel().isGreaterOrEqual(minimumLevel)) {
      String traceId = currentTraceId();
      if (traceId == null) return;
      if (!activeTraces.contains(traceId)) return;

      FirstNBuffer<BufferedLog> buf = buffers.computeIfAbsent(traceId, tid -> {
        if (bufferedTraceIds.size() >= maxActiveTraces && !bufferedTraceIds.contains(tid)) {
          return null;
        }
        bufferedTraceIds.add(tid);
        return new FirstNBuffer<>(perTraceCapacity);
      });
      if (buf == null) return;

      // If already full, skip extra work
      if (buf.isFull()) return;

      BufferedLog bl = BufferedLog.from(event);
      buf.add(bl);
    }
  }

  private String currentTraceId() {
    if (traceIdProvider != null) {
      String v = safeTrim(traceIdProvider.get());
      if (v != null && !v.isEmpty()) return v;
    }
    return safeTrim(MDC.get(mdcKey));
  }

  private static String safeTrim(String s) {
    return s == null ? null : s.trim();
  }

  // Data structures

  public static final class BufferedLog {
    public final Instant timestamp;
    public final String level;
    public final String logger;
    public final String thread;
    public final String message;
    public final String formattedMessage;
    public final String throwable;
    public final String traceId; // snapshot from MDC

    private BufferedLog(Instant timestamp, String level, String logger, String thread,
                        String message, String formattedMessage, String throwable, String traceId) {
      this.timestamp = timestamp;
      this.level = level;
      this.logger = logger;
      this.thread = thread;
      this.message = message;
      this.formattedMessage = formattedMessage;
      this.throwable = throwable;
      this.traceId = traceId;
    }

    public static BufferedLog from(ILoggingEvent e) {
      String throwableStr = null;
      if (e.getThrowableProxy() != null) {
        throwableStr = e.getThrowableProxy().getClassName() + ": " + e.getThrowableProxy().getMessage();
      }
      String traceIdSnapshot = Optional.ofNullable(MDC.getCopyOfContextMap())
          .map(m -> m.getOrDefault("traceId", null))
          .orElse(null);

      return new BufferedLog(
          Instant.ofEpochMilli(e.getTimeStamp()),
          e.getLevel().toString(),
          e.getLoggerName(),
          e.getThreadName(),
          e.getMessage(),
          e.getFormattedMessage(),
          throwableStr,
          traceIdSnapshot
      );
    }
  }

  /**
   * Collects only the first N items; once full, further adds are ignored.
   */
  static final class FirstNBuffer<T> {
    private final int capacity;
    private final ArrayList<T> data;
    private int size = 0;

    FirstNBuffer(int capacity) {
      this.capacity = capacity;
      this.data = new ArrayList<>(Math.min(capacity, 1024));
    }

    synchronized void add(T item) {
      if (size >= capacity) {
        return; // ignore further items
      }
      data.add(item);
      size++;
    }

    synchronized Collection<T> snapshot() {
      if (size == 0) return Collections.emptyList();
      return Collections.unmodifiableList(new ArrayList<>(data));
    }

    boolean isFull() {
      return size >= capacity;
    }
  }
}