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
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.SourceSql;
import uk.co.spudsoft.query.main.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
public class SourceSqlStreamingInstance implements SourceInstance<SourceSql> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlStreamingInstance.class);
  
  private final Vertx vertx;
  private final SourceSql definition;
  private RowStreamWrapper rowStreamWrapper;

  public SourceSqlStreamingInstance(Vertx vertx, Context context, SourceSql definition) {
    this.vertx = vertx;
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
    Pool pool = Pool.pool(vertx, connectOptions, definition.getPoolOptions() == null ? new PoolOptions() : definition.getPoolOptions());
    return pool.getConnection()
            .compose(conn -> {
              return prepareSqlStatement(conn);
            }).compose(ps -> {
              RowStream<Row> stream = ps.createStream(definition.getStreamingFetchSize());
              rowStreamWrapper = new RowStreamWrapper(stream);
              return Future.succeededFuture();
            }).mapEmpty()
            ;
    
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return rowStreamWrapper;
  }
  
}
