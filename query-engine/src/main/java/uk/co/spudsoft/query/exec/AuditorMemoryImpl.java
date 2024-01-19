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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.clientip;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.host;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.path;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.username;
import static uk.co.spudsoft.query.exec.AuditorPersistenceImpl.multiMapToJson;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.main.ExceptionToString;

/**
 *
 * @author jtalbut
 */
public class AuditorMemoryImpl implements Auditor {
  
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

    AuditRow(String id, LocalDateTime timestamp, String processId, String url, String clientIp, String host, String path, String arguments, String headers, String openIdDetails, String issuer, String subject, String username, String name, String groups) {
      this.id = id;
      this.timestamp = timestamp;
      this.processId = processId;
      this.url = url;
      this.clientIp = clientIp;
      this.host = host;
      this.path = path;
      this.arguments = arguments;
      this.headers = headers;
      this.openIdDetails = openIdDetails;
      this.issuer = issuer;
      this.subject = subject;
      this.username = username;
      this.name = name;
      this.groups = groups;
    }
  }
  
  private final Deque<AuditRow> auditRows = new ArrayDeque<>(SIZE + 1);
  
  @Override
  public void prepare() throws Exception {
  }
  
  private AuditRow find(String id) {
    if (Strings.isNullOrEmpty(id)) {
      return null;
    }
    synchronized (auditRows) {
      for (AuditRow row : auditRows) {
        if (id.equals(row.id)) {
          return row;
        }
      }
      logger.info("Did not find details of run {} in history of {} runs: {}", id, auditRows.size(), auditRows);
    }
    return null;
  }

  @Override
  public void recordException(RequestContext context, Throwable ex) {
    logger.info("Exception: {} {}", ex.getClass().getCanonicalName(), ex.getMessage());
    AuditRow row = find(context.getRequestId());
    if (row != null) {
      row.exceptionTime = LocalDateTime.now();
      row.exceptionClass = JdbcHelper.limitLength(ex.getClass().getCanonicalName(), 1000);
      row.exceptionMessage = JdbcHelper.limitLength(ex.getMessage(), 1000);
      row.exceptionStackTrace = ExceptionToString.convert(ex, "; ");
    }
  }

  @Override
  public void recordFileDetails(RequestContext context, DirCacheTree.File file) {
    logger.info("File: {} {} {}", file.getPath(), file.getSize(), file.getModified());
    AuditRow row = find(context.getRequestId());
    if (row != null) {
      row.filePath = JdbcHelper.limitLength(JdbcHelper.toString(file.getPath()), 1000);
      row.fileSize = file.getSize();
      if (file.getModified() != null) {
        row.fileModified = file.getModified();
      }
    }
  }

  @Override
  public Future<Void> recordRequest(RequestContext context) {
    JsonObject headers = multiMapToJson(context.getHeaders());
    JsonObject arguments = multiMapToJson(context.getArguments());
    JsonArray groups = Auditor.listToJson(context.getGroups());
    String openIdDetails = context.getJwt() == null ? null : context.getJwt().getPayloadAsString();

    logger.info("Request: {} {} {} {} {} {} {} {} {} {} {}",
             context.getUrl(),
             context.getClientIp(),
             context.getHost(),
             context.getPath(),
             arguments,
             headers,
             context.getIssuer(),
             context.getSubject(),
             context.getUsername(),
             context.getNameFromJwt(),
             context.getGroups()
    );
    
    AuditRow row = new AuditRow(
      JdbcHelper.limitLength(context.getRequestId(), 100)
      , LocalDateTime.now()
      , JdbcHelper.limitLength(PROCESS_ID, 1000)
      , JdbcHelper.limitLength(context.getUrl(), 1000)
      , JdbcHelper.limitLength(context.getClientIp().toNormalizedString(), 40)
      , JdbcHelper.limitLength(context.getHost(), 250)
      , JdbcHelper.limitLength(context.getPath(), 250)
      , JdbcHelper.toString(arguments)
      , JdbcHelper.toString(headers)
      , openIdDetails
      , JdbcHelper.limitLength(context.getIssuer(), 1000)
      , JdbcHelper.limitLength(context.getSubject(), 1000)
      , JdbcHelper.limitLength(context.getUsername(), 1000)
      , JdbcHelper.limitLength(context.getNameFromJwt(), 1000)
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
  
  static boolean rowMatches(RequestContext context, RateLimitRule rule, AuditRow row) {
    for (RateLimitScopeType scope : rule.getScope()) {
      switch (scope) {
        case clientip:
          if (!context.getClientIp().toNormalizedString().equals(row.clientIp)) {
            return false;
          }
          break;
        case host:
          if (!context.getHost().equals(row.host)) {
            return false;
          }
          break;
        case path:
          if (!context.getPath().equals(row.path)) {
            return false;
          }
          break;
        case username:
          if (!context.getUsername().equals(row.username)) {
            return false;
          }
          break;
        default:
         throw new IllegalStateException("Unknown RateLimitScope type: " + scope);
     }
    }
    return true;
  }
  
  private RateLimitCounts getCounts(RequestContext context, RateLimitRule rule, LocalDateTime startPoint) {
    RateLimitCounts result = new RateLimitCounts();
    synchronized (auditRows) {
      for (AuditRow row : auditRows) {
        if (startPoint.isAfter(row.timestamp)) {
          continue;
        }
        // Don't look at the current run
        if (context.getRequestId().equals(row.id)) {
          continue;
        }
        if (rowMatches(context, rule, row)) {
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
  public Future<Pipeline> runRateLimitRules(RequestContext context, Pipeline pipeline) {
    try {
      List<RateLimitRule> rules = pipeline.getRateLimitRules();

      logger.debug("Performing rate limit check with {} rules", rules.size());
      if (CollectionUtils.isEmpty(rules)) {
        return Future.succeededFuture(pipeline);
      }

      LocalDateTime now = LocalDateTime.now();
      Instant nowInstant = now.toInstant(ZoneOffset.UTC);

      for (int i = 0; i < rules.size(); ++i) {
        RateLimitRule rule = rules.get(i);
        if (rule.getTimeLimit() == null) {
          continue;
        }
        LocalDateTime startPoint = now.minus(rule.getTimeLimit());
        RateLimitCounts counts = getCounts(context, rule, startPoint);

        AuditorPersistenceImpl.evaluateRateLimitRule(rule, nowInstant, i
                , counts.outstanding, counts.total, counts.bytes, now);
      }
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture(pipeline);
  }
  
  @Override
  public void recordResponse(RequestContext context, HttpServerResponse response) {
    JsonObject headers = multiMapToJson(context.getHeaders());
    logger.info("Request complete: {} {} bytes in {}s {}"
            , response.getStatusCode()
            , response.bytesWritten()
            , (System.currentTimeMillis() - context.getStartTime()) / 1000.0
            , headers
    );
    AuditRow row = find(context.getRequestId());
    if (row != null) {
      row.responseTime = LocalDateTime.now();
      if (context.getHeadersSentTime() > 0) {
        row.responseStreamStartMillis = context.getHeadersSentTime() - context.getStartTime();
      }
      row.responseDurationMillis = System.currentTimeMillis() - context.getStartTime();
      row.responseCode = response.getStatusCode();
      row.responseRows = context.getRowsWritten();
      row.responseSize = response.bytesWritten();
      row.responseHeaders = JdbcHelper.toString(headers);
    }
  }

  @Override
  public Future<AuditHistory> getHistory(String issuer, String subject, int skipRows, int maxRows) {
    
    long count = 0;
    List<AuditHistoryRow> output = new ArrayList<>();
    for (AuditRow row : this.auditRows) {
      if (issuer.equals(row.issuer) && subject.equals(row.subject)) {
        ++count;
        if (count > skipRows && count < skipRows + maxRows) {
          ObjectNode arguments = null;
          try {
            if (!Strings.isNullOrEmpty(row.arguments)) {
              arguments = mapper.readValue(row.arguments, ObjectNode.class);
            }
          } catch (Throwable ex) {
            logger.warn("Arguments from database ({}) for id {} failed to parse: ", row.arguments.replaceAll("[\r\n]", ""), row.id, ex);
          }

          AuditHistoryRow ah = new AuditHistoryRow(
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
          output.add(ah);
        }
      }
    }
    
    return Future.succeededFuture(
              new AuditHistory(
                      skipRows
                      , count
                      , output
                      )
    );
  }
  
  @Override
  public void healthCheck(Promise<Status> promise) {
    promise.complete(Status.OK());
  }
}
