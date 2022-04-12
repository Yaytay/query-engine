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
import io.vertx.sqlclient.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.SourceSql;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineInstance;
import uk.co.spudsoft.query.main.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
public class SourceSqlStreamingInstance implements SourceInstance {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlStreamingInstance.class);
  
  private final Vertx vertx;
  private final SourceSql definition;
  private RowStreamWrapper rowStreamWrapper;
  
  private SqlConnection connection;
  private PreparedStatement preparedStatement;
  private Transaction transaction;

  private PoolCreator poolCreator = new PoolCreator();
  
  public SourceSqlStreamingInstance(Vertx vertx, Context context, SourceSql definition) {
    this.vertx = vertx;
    this.definition = definition;
  }

  void setPoolCreator(PoolCreator poolCreator) {
    this.poolCreator = poolCreator;
  }
  
  @SuppressFBWarnings(value = "SQL_INJECTION_VERTX", justification = "The query from the configuration is definitely a SQL injection vector, but it is not built from end-user input")
  Future<PreparedStatement> prepareSqlStatement(SqlConnection conn) {
    return conn.prepare(definition.getQuery());
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    
    Endpoint endpoint = pipeline.getSourceEndpoints().get(definition.getEndpoint());
    if (endpoint == null) {
      return Future.failedFuture("Endpoint \"" + definition.getEndpoint() + "\" not found");
    }
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(endpoint.getUrl());
    if (!Strings.isNullOrEmpty(endpoint.getUsername())) {
      connectOptions.setUser(endpoint.getUsername());
    }
    if (!Strings.isNullOrEmpty(endpoint.getPassword())) {
      connectOptions.setPassword(endpoint.getPassword());
    }
    Pool pool = poolCreator.pool(vertx, connectOptions, definition.getPoolOptions() == null ? new PoolOptions().setMaxSize(1) : definition.getPoolOptions());
    return pool.getConnection()
            .compose(conn -> {
              connection = conn;
              return prepareSqlStatement(connection);
            }).compose(ps -> {
              preparedStatement = ps;
              return connection.begin();
            }).compose(tran -> {
              transaction = tran;
              logger.trace("Creating SQL stream on {}", connection);
              RowStream<Row> stream = preparedStatement.createStream(definition.getStreamingFetchSize());
              rowStreamWrapper = new RowStreamWrapper(transaction, stream);
              return Future.succeededFuture();
            }).mapEmpty()
            ;    
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return rowStreamWrapper;
  }
  
}