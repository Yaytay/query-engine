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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.Status;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import uk.co.spudsoft.query.exec.context.RequestContext;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.clientip;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.host;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.issuer;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.path;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.subject;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.username;
import static uk.co.spudsoft.query.exec.AuditHistorySortOrder.id;
import static uk.co.spudsoft.query.exec.AuditHistorySortOrder.responseStreamStart;
import static uk.co.spudsoft.query.exec.AuditHistorySortOrder.timestamp;
import static uk.co.spudsoft.query.exec.AuditorPersistenceImpl.multiMapToJson;
import static uk.co.spudsoft.query.exec.AuditorPersistenceImpl.requestId;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.ExceptionToString;

/**
 * Audit implementation that is based on a size-constrained list in memory.
 * This is not intended for production use, but can be a good choice for design mode.
 * @author jtalbut
 */
public class AuditorMemoryImpl implements Auditor {

  /**
   * The maximum number of audit rows to hold in memory.
   */
  public static final int SIZE = 200;
  private static final String PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName();

  private static final Logger logger = LoggerFactory.getLogger(AuditorMemoryImpl.class);

  private final ObjectMapper mapper = DatabindCodec.mapper();

  @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Many fields here only exist for synchronicity with the persistence auditor")
  static class AuditRow {
    private String id;
    private LocalDateTime timestamp;
    private String processId;
    private String url;
    private String clientIp;
    private String host;
    private String path;
    private String arguments;
    private String headers;
    private String openIdDetails;
    private String issuer;
    private String subject;
    private String username;
    private String name;
    private String groups;
    private String filePath;
    private Long fileSize;
    private LocalDateTime fileModified;
    private String fileHash;
    private LocalDateTime exceptionTime;
    private String exceptionClass;
    private String exceptionMessage;
    private String exceptionStackTrace;
    private LocalDateTime responseTime;
    private Long responseStreamStartMillis;
    private Long responseDurationMillis;
    private Integer responseCode;
    private Long responseRows;
    private Long responseSize;
    private String responseHeaders;
    private String cacheKey;
    private LocalDateTime cacheExpiry;
    private LocalDateTime cacheDeleted;
    private String cacheFile;

    AuditRow(String id, LocalDateTime timestamp, String processId, String url, String clientIp, String host, String path, String arguments, String headers) {
      this.id = id;
      this.timestamp = timestamp;
      this.processId = processId;
      this.url = url;
      this.clientIp = clientIp;
      this.host = host;
      this.path = path;
      this.arguments = arguments;
      this.headers = headers;
    }

    void setTokenDetails(String openIdDetails, String issuer, String subject, String username, String name, String groups) {
      this.openIdDetails = openIdDetails;
      this.issuer = issuer;
      this.subject = subject;
      this.username = username;
      this.name = name;
      this.groups = groups;
    }
  }

  private final Vertx vertx;
  private final Deque<AuditRow> auditRows = new ArrayDeque<>(SIZE + 1);

  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   */
  public AuditorMemoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void prepare() throws Exception {
  }

  @Override
  public Future<Void> waitForOutstandingRequests(long timeoutMs) {
    Promise<Void> promise = Promise.promise();
    waitForOutstandingRequests(promise, System.currentTimeMillis() + timeoutMs);
    return promise.future();
  }

  private void waitForOutstandingRequests(Promise<Void> promise, long terminalTime) {
    boolean allComplete = true;
    for (AuditRow row : auditRows) {
      if (row.responseTime == null) {
        allComplete = false;
        break;
      }
    }
    if (allComplete) {
      promise.complete();
    } else if (System.currentTimeMillis() > terminalTime) {
      promise.fail(new TimeoutException("Outstanding requests remain after timeout"));
    } else {
      vertx.setTimer(500, l -> {
        waitForOutstandingRequests(promise, terminalTime);
      });
    }
  }

  private AuditRow find(RequestContext requestContext) {
    String id = requestId(requestContext);
    if (Strings.isNullOrEmpty(id)) {
      return null;
    }
    synchronized (auditRows) {
      for (AuditRow row : auditRows) {
        if (id.equals(row.id)) {
          return row;
        }
      }
      Log.decorate(logger.atInfo(), requestContext).log("Did not find details of run {} in history of {} runs: {}", id, auditRows.size(), auditRows);
    }
    return null;
  }

  @Override
  @SuppressFBWarnings(value = "UNSAFE_HASH_EQUALS", justification = "The user has no control over the hash and production instances should be using AuditorPersistenceImpl")
  public Future<CacheDetails> getCacheFile(RequestContext requestContext, Pipeline pipeline) {

    String cacheKey = AuditorPersistenceImpl.buildCacheKey(requestContext);
    if (cacheKey != null) {
      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
      synchronized (auditRows) {
        for (AuditRow row : auditRows) {
          if (cacheKey.equals(row.cacheKey)
                  && row.cacheExpiry != null
                  && row.cacheExpiry.isAfter(now)
                  && row.cacheDeleted == null
                  && row.fileHash != null
                  && row.fileHash.equals(pipeline.getSha256())
                  ) {
            return Future.succeededFuture(new CacheDetails(row.id, row.cacheFile, row.cacheExpiry));
          }
        }
      }
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> recordCacheFile(RequestContext requestContext, String fileName, LocalDateTime expiry) {
    String requestId = requestId(requestContext);
    if (requestId != null) {
      synchronized (auditRows) {
        for (AuditRow row : auditRows) {
          if (requestId.equals(row.id)) {
            row.cacheKey = AuditorPersistenceImpl.buildCacheKey(requestContext);
            row.cacheFile = fileName;
            row.cacheExpiry = expiry;
            break;
          }
        }
        return Future.succeededFuture();
      }
    } else {
      return Future.failedFuture("RequestContext not set");
    }
  }

  @Override
  public Future<Void> recordCacheFileUsed(RequestContext requestContext, String fileName) {
    String requestId = requestId(requestContext);
    if (requestId != null) {
      synchronized (auditRows) {
        for (AuditRow row : auditRows) {
          if (requestId.equals(row.id)) {
            row.cacheFile = fileName;
            break ;
          }
        }
        return Future.succeededFuture();
      }
    } else {
      return Future.failedFuture("RequestContext not set");
    }
  }

  @Override
  public Future<Void> deleteCacheFile(RequestContext requestContext, String auditId) {

    synchronized (auditRows) {
      for (AuditRow row : auditRows) {
        if (auditId.equals(row.id)) {
          row.cacheDeleted = LocalDateTime.now(ZoneOffset.UTC);
        }
      }
    }
    return Future.succeededFuture();

  }

  @Override
  public void recordException(RequestContext requestContext, Throwable ex) {
    Log.decorate(logger.atInfo(), requestContext)
            .setMessage("Exception: {} {}")
            .addArgument(ex.getClass().getCanonicalName())
            .addArgument(ex.getMessage())
            .setCause(ex)
            .addKeyValue(PROCESS_ID, SIZE)
            .log();
    AuditRow row = find(requestContext);
    if (row != null) {
      row.exceptionTime = LocalDateTime.now(ZoneOffset.UTC);
      row.exceptionClass = JdbcHelper.limitLength(ex.getClass().getCanonicalName(), 1000);
      row.exceptionMessage = JdbcHelper.limitLength(ex.getMessage(), 1000);
      row.exceptionStackTrace = ExceptionToString.convert(ex, "; ");
    }
  }

  @Override
  public Future<Void> recordFileDetails(RequestContext requestContext, DirCacheTree.File file, Pipeline pipeline) {
    Log.decorate(logger.atInfo(), requestContext)
            .log("File: {} {} {}", file.getPath(), file.getSize(), file.getModified());
    AuditRow row = find(requestContext);
    if (row != null) {
      row.filePath = JdbcHelper.limitLength(JdbcHelper.toString(file.getPath()), 1000);
      row.fileSize = file.getSize();
      if (file.getModified() != null) {
        row.fileModified = file.getModified();
      }
      if (pipeline != null) {
        row.fileHash = pipeline.getSha256();
      }
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> recordRequest(RequestContext requestContext) {
    if (requestContext == null) {
      return Future.failedFuture("RequestContext not set");
    }
             
    JsonObject headers = multiMapToJson(requestContext, requestContext.getHeaders());
    JsonObject arguments = multiMapToJson(requestContext, requestContext.getParams());

    Log.decorate(logger.atInfo(), requestContext).log("Request: {} {} {} {} {} {} {} {} {} {} {}",
             requestContext.getUrl(),
             requestContext.getClientIp(),
             requestContext.getHost(),
             requestContext.getPath(),
             arguments,
             headers
    );

    AuditRow row = new AuditRow(
      JdbcHelper.limitLength(requestContext.getRequestId(), 100)
      , LocalDateTime.now(ZoneOffset.UTC)
      , JdbcHelper.limitLength(PROCESS_ID, 1000)
      , JdbcHelper.limitLength(requestContext.getUrl(), 1000)
      , JdbcHelper.limitLength(requestContext.getClientIp().toNormalizedString(), 40)
      , JdbcHelper.limitLength(requestContext.getHost(), 250)
      , JdbcHelper.limitLength(requestContext.getPath(), 250)
      , JdbcHelper.toString(arguments)
      , JdbcHelper.toString(headers)
    );

    JsonArray groups = Auditor.listToJson(requestContext.getGroups());
    String openIdDetails = requestContext.getJwt() == null ? null : requestContext.getJwt().getPayloadAsString();

    row.setTokenDetails(openIdDetails
            , JdbcHelper.limitLength(requestContext.getIssuer(), 1000)
            , JdbcHelper.limitLength(requestContext.getSubject(), 1000)
            , JdbcHelper.limitLength(requestContext.getUsername(), 1000)
            , JdbcHelper.limitLength(requestContext.getName(), 1000)
            , JdbcHelper.toString(groups)
    );

    synchronized (auditRows) {
      auditRows.addLast(row);
      if (auditRows.size() > SIZE) {
        auditRows.removeFirst();
      }
    }
    return Future.succeededFuture();
  }

  private static class RateLimitCounts {
    private int outstanding;
    private int total;
    private long bytes;
  }

  static boolean rowMatches(RequestContext requestContext, RateLimitRule rule, AuditRow row) {
    if (requestContext == null) {
      return false;
    }
             
    for (RateLimitScopeType scope : rule.getScope()) {
      switch (scope) {
        case clientip:
          if (!requestContext.getClientIp().toNormalizedString().equals(row.clientIp)) {
            return false;
          }
          break;
        case host:
          if (requestContext.getHost() == null || !requestContext.getHost().equals(row.host)) {
            return false;
          }
          break;
        case path:
          if (requestContext.getPath() == null || !requestContext.getPath().equals(row.path)) {
            return false;
          }
          break;
        case issuer:
          if (requestContext.getIssuer() == null || !requestContext.getIssuer().equals(row.issuer)) {
            return false;
          }
          break;
        case subject:
          if (requestContext.getSubject() == null || !requestContext.getSubject().equals(row.subject)) {
            return false;
          }
          break;
        case username:
          if (requestContext.getUsername() == null || !requestContext.getUsername().equals(row.username)) {
            return false;
          }
          break;
        default:
         throw new IllegalStateException("Unknown RateLimitScope type: " + scope);
     }
    }
    return true;
  }

  private RateLimitCounts getCounts(RequestContext requestContext, RateLimitRule rule, LocalDateTime startPoint) {
    RateLimitCounts result = new RateLimitCounts();
    synchronized (auditRows) {
      for (AuditRow row : auditRows) {
        if (startPoint.isAfter(row.timestamp)) {
          continue;
        }
        // Don't look at the current run
        if (requestContext.getRequestId().equals(row.id)) {
          continue;
        }
        if (rowMatches(requestContext, rule, row)) {
          result.total += 1;
          if (row.responseTime != null) {
            result.bytes += row.responseSize;
          } else {
            result.outstanding += 1;
          }
        }
      }
    }
    return result;
  }

  @Override
  public Future<Pipeline> runRateLimitRules(RequestContext requestContext, Pipeline pipeline) {
    if (requestContext == null) {
      return Future.failedFuture("RequestContext not set");
    }

    try {
      List<RateLimitRule> rules = pipeline.getRateLimitRules();

      Log.decorate(logger.atDebug(), requestContext).log("Performing rate limit check with {} rules", rules.size());
      if (CollectionUtils.isEmpty(rules)) {
        return Future.succeededFuture(pipeline);
      }

      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
      Instant nowInstant = now.toInstant(ZoneOffset.UTC);

      for (int i = 0; i < rules.size(); ++i) {
        RateLimitRule rule = rules.get(i);
        if (rule.getTimeLimit() == null) {
          continue;
        }
        LocalDateTime startPoint = now.minus(rule.getTimeLimit());
        RateLimitCounts counts = getCounts(requestContext, rule, startPoint);

        AuditorPersistenceImpl.evaluateRateLimitRule(requestContext, rule, nowInstant, i
                , counts.outstanding, counts.total, counts.bytes, now);
      }
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture(pipeline);
  }

  @Override
  public void recordResponse(RequestContext requestContext, HttpServerResponse response) {
    if (requestContext == null) {
      return ;
    }

    JsonObject headers = multiMapToJson(requestContext, response.headers());
    Log.decorate(logger.atInfo(), requestContext)
            .log("Request complete: {} {} bytes in {}s {}"
            , response.getStatusCode()
            , response.bytesWritten()
            , (System.currentTimeMillis() - requestContext.getStartTime()) / 1000.0
            , headers
    );
    AuditRow row = find(requestContext);
    if (row != null) {
      row.responseTime = LocalDateTime.now(ZoneOffset.UTC);
      if (requestContext.getHeadersSentTime() > 0) {
        row.responseStreamStartMillis = requestContext.getHeadersSentTime() - requestContext.getStartTime();
      }
      row.responseDurationMillis = System.currentTimeMillis() - requestContext.getStartTime();
      row.responseCode = response.getStatusCode();
      row.responseRows = requestContext.getRowsWritten();
      row.responseSize = response.bytesWritten();
      row.responseHeaders = JdbcHelper.toString(headers);
    }
  }

  static <T extends Comparable<T>> Comparator<T> buildComparator(boolean sortDescending) {
    Comparator<T> comparator = Comparator.nullsLast(Comparator.naturalOrder());
    if (sortDescending) {
      comparator = Collections.reverseOrder(comparator);
    }
    return comparator;
  }

  static Comparator<AuditHistoryRow> createComparator(AuditHistorySortOrder sortOrder, boolean sortDescending) {

    switch (sortOrder) {
      case timestamp -> {
        return Comparator.comparing(ahr -> ahr.getTimestamp(), buildComparator(sortDescending));
      }
      case id -> {
        return Comparator.comparing(ahr -> ahr.getId(), buildComparator(sortDescending));
      }
      case path -> {
        return Comparator.comparing(ahr -> ahr.getPath(), buildComparator(sortDescending));
      }
      case host -> {
        return Comparator.comparing(ahr -> ahr.getHost(), buildComparator(sortDescending));
      }
      case issuer -> {
        return Comparator.comparing(ahr -> ahr.getIssuer(), buildComparator(sortDescending));
      }
      case subject -> {
        return Comparator.comparing(ahr -> ahr.getSubject(), buildComparator(sortDescending));
      }
      case username -> {
        return Comparator.comparing(ahr -> ahr.getUsername(), buildComparator(sortDescending));
      }
      case name -> {
        return Comparator.comparing(ahr -> ahr.getName(), buildComparator(sortDescending));
      }
      case responseCode -> {
        return Comparator.comparing(ahr -> ahr.getResponseCode(), buildComparator(sortDescending));
      }
      case responseRows -> {
        return Comparator.comparing(ahr -> ahr.getResponseRows(), buildComparator(sortDescending));
      }
      case responseSize -> {
        return Comparator.comparing(ahr -> ahr.getResponseSize(), buildComparator(sortDescending));
      }
      case responseStreamStart -> {
        return Comparator.comparing(ahr -> ahr.getResponseStreamStart(), buildComparator(sortDescending));
      }
      case responseDuration -> {
        return Comparator.comparing(ahr -> ahr.getResponseDuration(), buildComparator(sortDescending));
      }
      default ->
        throw new IllegalStateException("Unable to sort by " + sortOrder + ".");
    }
  }

  @Override
  public Future<AuditHistory> getHistory(RequestContext requestContext, String issuer, String subject, int skipRows, int maxRows, AuditHistorySortOrder sortOrder, boolean sortDescending) {

    long count[] = {0};
    List<AuditHistoryRow> output = this.auditRows.stream()
            .filter(ar -> Objects.equals(issuer, ar.issuer) && Objects.equals(subject, ar.subject))
            .map(row -> mapAuditHistoryRow(requestContext, row))
            .sorted(createComparator(sortOrder, sortDescending))
            .filter(ar -> {
              count[0]++;
              return (count[0] > skipRows && count[0] < skipRows + maxRows);
            })
            .collect(Collectors.toList())
            ;

    return Future.succeededFuture(
              new AuditHistory(
                      skipRows
                      , count[0]
                      , output
                      )
    );
  }

  private AuditHistoryRow mapAuditHistoryRow(RequestContext requestContext, AuditRow row) {
    ObjectNode arguments = null;
    try {
      if (!Strings.isNullOrEmpty(row.arguments)) {
        arguments = mapper.readValue(row.arguments, ObjectNode.class);
      }
    } catch (Throwable ex) {
      Log.decorate(logger.atWarn(), requestContext)
              .log("Arguments from database ({}) for id {} failed to parse: ", row.arguments.replaceAll("[\r\n]", ""), row.id, ex);
    }

    return new AuditHistoryRow(
            row.timestamp
            , row.id
            , row.path
            , arguments
            , row.host
            , row.issuer
            , row.subject
            , row.username
            , row.name
            , row.responseCode
            , row.responseRows
            , row.responseSize
            , row.responseStreamStartMillis
            , row.responseDurationMillis
    );
  }

  @Override
  public void healthCheck(Promise<Status> promise) {
    promise.complete(Status.OK());
  }
}
