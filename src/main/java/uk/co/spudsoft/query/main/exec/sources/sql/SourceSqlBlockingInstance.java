/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.SqlResult;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.SourceSql;
import uk.co.spudsoft.query.main.exec.SourceInstance;



/**
 *
 * @author jtalbut
 */
public class SourceSqlBlockingInstance implements SourceInstance<SourceSql> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlBlockingInstance.class);
  
  private final Vertx vertx;
  private final BlockingReadStream<JsonObject> stream;
  private final SourceSql definition;

  public SourceSqlBlockingInstance(Vertx vertx, Context context, SourceSql definition) {
    this.vertx = vertx;
    this.stream = new BlockingReadStream<>(context, definition.getRowsQueuedBeforeDiscard());
    this.definition = definition;
  }
  
  @SuppressFBWarnings(value = "SQL_INJECTION_VERTX", justification = "The query from the configuration is definitely a SQL injection vector, but it is not built from end-user input")
  private Future<PreparedStatement> prepareSqlStatement(SqlConnection conn) {
    return conn.prepare(definition.getQuery());
  }
  
  @Override
  public Future<Void> initialize(Map<String, Endpoint> endpoints) {
    
    Endpoint endpoint = endpoints.get(definition.getEndpoint());
    if (endpoint == null) {
      return Future.failedFuture("Endpoint not found");
    }
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(endpoint.getUrl());
    if (!Strings.isNullOrEmpty(endpoint.getUsername())) {
      connectOptions.setUser(endpoint.getUsername());
    }
    if (!Strings.isNullOrEmpty(endpoint.getPassword())) {
      connectOptions.setPassword(endpoint.getPassword());
    }
    Pool pool;
    try {
      pool = Pool.pool(vertx, connectOptions, definition.getPoolOptions() == null ? new PoolOptions() : definition.getPoolOptions());
    } catch(Throwable ex) {
      return Future.failedFuture(ex);
    }
    return pool.getConnection()
            .compose(conn -> {
              return prepareSqlStatement(conn);
            }).compose(ps -> {
                PreparedQuery<RowSet<Row>> pq = ps.query();
                Collector<Row, ?, Integer> collector = Collectors.summingInt(row -> {
                  JsonObject jo = row.toJson();
                  try {
                    stream.add(jo);
                  } catch(Throwable ex) {
                    logger.debug("{}: Failed to add to stream: ", endpoint.getUrl(), ex);                              
                  }
                  return 1;
                });
                PreparedQuery<SqlResult<Integer>> pq2 = pq.collecting(collector);
                pq2.execute()
                        .onSuccess(sr -> {
                          stream.end();
                        })
                        ;
                return Future.succeededFuture();
            }).mapEmpty()
            ;
    
    
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return stream;
  }
  
  
}
