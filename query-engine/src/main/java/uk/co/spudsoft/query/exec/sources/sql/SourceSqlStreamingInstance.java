/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.sources.AbstractSource;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
public class SourceSqlStreamingInstance extends AbstractSource {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlStreamingInstance.class);
  
  private final Vertx vertx;
  private final PoolCreator poolCreator;  
  private final SourceSql definition;
  private RowStreamWrapper rowStreamWrapper;
  
  private SqlConnection connection;
  private PreparedStatement preparedStatement;
  private Transaction transaction;

  
  public SourceSqlStreamingInstance(Vertx vertx, Context context, SharedMap sharedMap, SourceSql definition, String defaultName) {
    super(Strings.isNullOrEmpty(definition.getName()) ? defaultName : definition.getName());
    this.vertx = vertx;
    
    Object pco = sharedMap.get(PoolCreator.class.toString());
    if (pco instanceof PoolCreator pc) {
      this.poolCreator = pc;
    } else {
      this.poolCreator = new PoolCreator();
      sharedMap.put(PoolCreator.class.toString(), this.poolCreator);
    }

    this.definition = definition;
  }

  @SuppressFBWarnings(value = "SQL_INJECTION_VERTX", justification = "The query from the configuration is definitely a SQL injection vector, but it is not built from end-user input")
  Future<PreparedStatement> prepareSqlStatement(SqlConnection conn, String sql) {
    return conn.prepare(sql);
  }
  
  private AbstractSqlPreparer getPreparer(String url) {
    if (url.startsWith("sqlserver")) {
      return new MsSqlPreparer();
    } else if (url.startsWith("postgresql")) {
      return new PostgreSqlPreparer();
    } else if (url.startsWith("mysql")) {
      return new MySqlPreparer();
    } else {
      return new MsSqlPreparer();
    }
  }
  
  private PoolOptions poolOptions(SourceSql definition) {
    PoolOptions po = new PoolOptions();

    return po;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {

    RequestContext requestContext = vertx.getOrCreateContext().getLocal("ctx");
    
    String endpointName = definition.getEndpoint();
    if (Strings.isNullOrEmpty(endpointName)) {
      endpointName = pipeline.renderTemplate(definition.getEndpointTemplate());
    }    
    Endpoint endpoint = pipeline.getSourceEndpoints().get(endpointName);
    if (endpoint == null) {
      return Future.failedFuture(new ServiceException(400, "Endpoint \"" + definition.getEndpoint() + "\" not found in " + pipeline.getSourceEndpoints().keySet()));
    }
    if (!ConditionInstance.isNullOrBlank(endpoint.getCondition())) {
      ConditionInstance cond = endpoint.getCondition().createInstance();
      if (!cond.evaluate(requestContext)) {
        String message = String.format("Endpoint %s (%s) rejected by condition (%s)", definition.getEndpoint(), endpoint.getUrl(), endpoint.getCondition());
        logger.warn(message);
        return Future.failedFuture(new ServiceException(503, "Endpoint \"" + definition.getEndpoint() + "\" not accessible", new IllegalStateException(message)));
      }
    }
    
    String url = endpoint.getUrl();
    if (Strings.isNullOrEmpty(url)) {
      url = pipeline.renderTemplate(endpoint.getUrlTemplate());
    }
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(url);
    try {
      processCredentials(endpoint, connectOptions, executor, requestContext);
    } catch (ServiceException ex) {
      return Future.failedFuture(ex);
    }
    Pool pool = poolCreator.pool(vertx, connectOptions, poolOptions(definition));
    Context outerContext = vertx.getOrCreateContext();
    logger.trace("Outer context: {}", outerContext);
    
    AbstractSqlPreparer preparer = getPreparer(url);
    AbstractSqlPreparer.QueryAndArgs queryAndArgs = preparer.prepareSqlStatement(definition.getQuery(), definition.getReplaceDoubleQuotes(), pipeline.getArguments());
    String sql = queryAndArgs.query;
    Tuple args = Tuple.from(queryAndArgs.args);
    
    return pool.getConnection()
            .recover(ex -> {
              if (ex instanceof ServiceException) {
                return Future.failedFuture(ex);
              } else {
                return Future.failedFuture(new ServiceException(500, "Failed to connect to data source", ex));
              }
            })
            .compose(conn -> {
              connection = conn;
              return prepareSqlStatement(conn, sql);
            }).compose(ps -> {
              preparedStatement = ps;
              return connection.begin();
            }).compose(tran -> {
              transaction = tran;
              logger.trace("Creating SQL stream on {}", connection);
              RowStream<Row> stream = preparedStatement.createStream(definition.getStreamingFetchSize(), args);
              rowStreamWrapper = new RowStreamWrapper(this, connection, transaction, stream);
              Context innerContext = vertx.getOrCreateContext();
              logger.trace("Outer context: {}", outerContext);
              logger.trace("Inner context: {}", innerContext);
              return Future.succeededFuture();
            })
            .recover(ex -> {
              if (ex instanceof ServiceException) {
                return Future.failedFuture(ex);
              } else {
                return Future.failedFuture(new ServiceException(500, "Failed to execute query", ex));
              }
            })
            .onFailure(ar -> {
              if (connection != null) {
                connection.close();
              }
            })
            .mapEmpty()
            ;
  }
  
  private String coalesce(String one, String two) {
    if (one == null) {
      return two;
    } else {
      return one;
    }
  }

  private void processCredentials(Endpoint endpoint, SqlConnectOptions connectOptions, PipelineExecutor executor, RequestContext requestContext) throws ServiceException {
    if (Strings.isNullOrEmpty(endpoint.getSecret())) {
      if (!Strings.isNullOrEmpty(endpoint.getUsername())) {
        connectOptions.setUser(endpoint.getUsername());
      }
      if (!Strings.isNullOrEmpty(endpoint.getPassword())) {
        connectOptions.setPassword(endpoint.getPassword());
      }
    } else {
      ProtectedCredentials credentials = executor.getSecret(endpoint.getSecret());
      if (credentials == null) {
        String message = String.format("Endpoint %s (%s) requires secret %s which does not exist", definition.getEndpoint(), coalesce(endpoint.getUrl(), endpoint.getUrlTemplate()), endpoint.getSecret());
        logger.warn(message);
        throw new ServiceException(503, "Endpoint \"" + definition.getEndpoint() + "\" not accessible", new IllegalStateException(message));
      }
      if (!ConditionInstance.isNullOrBlank(credentials.getCondition())) {
        ConditionInstance cond = credentials.getCondition().createInstance();
        if (!cond.evaluate(requestContext)) {
          String message = String.format("Endpoint %s (%s) prevented from accessing secret %s by condition (%s)", definition.getEndpoint(), coalesce(endpoint.getUrl(), endpoint.getUrlTemplate()), endpoint.getSecret(), endpoint.getCondition());
          logger.warn(message);
          throw new ServiceException(503, "Endpoint \"" + definition.getEndpoint() + "\" not accessible", new IllegalStateException(message));
        }
      }
      if (!Strings.isNullOrEmpty(credentials.getUsername())) {
        connectOptions.setUser(credentials.getUsername());
      }
      if (!Strings.isNullOrEmpty(credentials.getPassword())) {
        connectOptions.setPassword(credentials.getPassword());
      }
    }
  }

  @Override
  public ReadStream<DataRow> getReadStream() {
    return rowStreamWrapper;
  }
  
}
