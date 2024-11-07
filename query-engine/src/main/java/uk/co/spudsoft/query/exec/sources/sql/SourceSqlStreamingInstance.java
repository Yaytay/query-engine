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
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.sources.AbstractSource;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.web.RequestContextHandler;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * {@link uk.co.spudsoft.query.exec.SourceInstance} class for SQL.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.SourceSql} object, that may reference {@link uk.co.spudsoft.query.main.ProtectedCredentials} configured globally.
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

  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   * @param context The Vert.x context.
   * @param sharedMap Pooling map.
   * @param definition The {@link SourceSql} definition.
   * @param defaultName The name to use for the SourceInstance if no other name is provided in the definition.
   */
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
  
  static AbstractSqlPreparer getPreparer(String url) {
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
  
  static PoolOptions poolOptions(SourceSql definition) {
    PoolOptions po = new PoolOptions();
    if (definition.getConnectionTimeout() != null) {
      long millis = definition.getConnectionTimeout().toMillis();
      if (millis > Integer.MAX_VALUE) {
        millis = Integer.MAX_VALUE;
      }
      po.setConnectionTimeout((int) millis);
      po.setConnectionTimeoutUnit(TimeUnit.MILLISECONDS);
    }
    return po;
  }
  
  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline) {

    RequestContext requestContext = RequestContextHandler.getRequestContext(vertx.getOrCreateContext());
    this.addNameToContextLocalData();
    
    String endpointName = definition.getEndpoint();
    if (Strings.isNullOrEmpty(endpointName)) {
      try {
        endpointName = pipeline.renderTemplate(definition.getName() + ":endpoint", definition.getEndpointTemplate());
      } catch (Throwable ex) {
        logger.warn("Failed to render endpoint template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }    
    Endpoint endpoint = pipeline.getSourceEndpoints().get(endpointName);
    if (endpoint == null) {
      return Future.failedFuture(new ServiceException(400, "Endpoint \"" + definition.getEndpoint() + "\" not found in " + pipeline.getSourceEndpoints().keySet()));
    }
    if (!JexlEvaluator.isNullOrBlank(endpoint.getCondition())) {
      ConditionInstance cond = endpoint.getCondition().createInstance();
      if (!cond.evaluate(requestContext, null)) {
        String message = String.format("Endpoint %s (%s) rejected by condition (%s)", definition.getEndpoint(), endpoint.getUrl(), endpoint.getCondition());
        logger.warn(message);
        return Future.failedFuture(new ServiceException(503, "Endpoint \"" + definition.getEndpoint() + "\" not accessible", new IllegalStateException(message)));
      }
    }
    
    String url = endpoint.getUrl();
    if (Strings.isNullOrEmpty(url)) {
      try {
        url = pipeline.renderTemplate(definition.getName() + ":url", endpoint.getUrlTemplate());
      } catch (Throwable ex) {
        logger.warn("Failed to render url template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(url);
    try {
      processCredentials(endpoint, connectOptions, executor, requestContext);
    } catch (ServiceException ex) {
      return Future.failedFuture(ex);
    }
    Pool pool = poolCreator.pool(vertx, connectOptions, poolOptions(definition));
    Context context = vertx.getOrCreateContext();
    logger.trace("Outer context: {}", context);
    
    String query = definition.getQuery();
    if (!Strings.isNullOrEmpty(definition.getQueryTemplate())) {
      try {
        query = pipeline.renderTemplate(definition.getName() + ":query", definition.getQueryTemplate());
      } catch (Throwable ex) {
        logger.warn("Failed to render query template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    
    AbstractSqlPreparer preparer = getPreparer(url);
    AbstractSqlPreparer.QueryAndArgs queryAndArgs = preparer.prepareSqlStatement(query, definition.getReplaceDoubleQuotes(), pipeline.getArgumentInstances());
    String sql = queryAndArgs.query();
    Tuple args = Tuple.from(queryAndArgs.args());
    
    return pool.getConnection()
            .recover(ex -> {
              return Future.failedFuture(new ServiceException(500, "Failed to connect to data source", ex));
            })
            .compose(conn -> {
              connection = conn;
              addNameToContextLocalData();
              logger.info("Preparing SQL: {}", sql);
              return prepareSqlStatement(conn, sql);
            }).compose(ps -> {
              addNameToContextLocalData();
              preparedStatement = ps;
              return connection.begin();
            }).compose(tran -> {
              addNameToContextLocalData();
              transaction = tran;
              logger.debug("Executing SQL stream on {} with {}", connection, args);
              MetadataRowStreamImpl rowStream = new MetadataRowStreamImpl(preparedStatement, context, definition.getStreamingFetchSize(), args);
              rowStreamWrapper = new RowStreamWrapper(this, connection, transaction, rowStream);
              return rowStreamWrapper.ready();
            })
            .recover(ex -> {
              addNameToContextLocalData();
              return Future.failedFuture(ServiceException.rethrowOrWrap(ex));
            })
            .onFailure(ex -> {
              addNameToContextLocalData();
              logger.warn("SQL source failed: ", ex);
              if (connection != null) {
                connection.close();
              }
            })
            .map(v -> new ReadStreamWithTypes(rowStreamWrapper, rowStreamWrapper.getTypes()))
            ;
  }
  
  static String coalesce(String one, String two) {
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
      if (!JexlEvaluator.isNullOrBlank(credentials.getCondition())) {
        ConditionInstance cond = credentials.getCondition().createInstance();
        if (!cond.evaluate(requestContext, null)) {
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

}
