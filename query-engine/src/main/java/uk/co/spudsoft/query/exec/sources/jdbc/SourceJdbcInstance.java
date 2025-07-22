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
package uk.co.spudsoft.query.exec.sources.jdbc;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.SourceJdbc;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.sources.AbstractSource;
import uk.co.spudsoft.query.exec.sources.sql.AbstractSqlPreparer;
import uk.co.spudsoft.query.exec.sources.sql.SourceSqlStreamingInstance;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.web.RequestContextHandler;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * {@link uk.co.spudsoft.query.exec.SourceInstance} class for SQL using JDBC.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.SourceJdbc} object, that may reference {@link uk.co.spudsoft.query.main.ProtectedCredentials} configured globally.
 * 
 * @author jtalbut
 */
public class SourceJdbcInstance extends AbstractSource {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlStreamingInstance.class);
  
  private final Vertx vertx;
  private final Context context;
  private final SourceJdbc definition;
  
  private JdbcReadStream jdbcReadStream;
  
  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   * @param context The Vert.x context.
   * @param definition The {@link SourceSql} definition.
   * @param defaultName The name to use for the SourceInstance if no other name is provided in the definition.
   */
  public SourceJdbcInstance(Vertx vertx, Context context, SourceJdbc definition, String defaultName) {
    super(Strings.isNullOrEmpty(definition.getName()) ? defaultName : definition.getName());
    this.vertx = vertx;
    this.context = context;
    this.definition = definition;    
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
      return Future.failedFuture(new ServiceException(400, "Endpoint \"" + endpointName + "\" not found in " + pipeline.getSourceEndpoints().keySet()));
    }
    if (!JexlEvaluator.isNullOrBlank(endpoint.getCondition())) {
      ConditionInstance cond = endpoint.getCondition().createInstance();
      if (!cond.evaluate(requestContext, null)) {
        String message = String.format("Endpoint %s (%s) rejected by condition (%s)", endpointName, endpoint.getUrl(), endpoint.getCondition());
        logger.warn("Endpoint {} ({}) rejected by condition ({})", endpointName, endpoint.getUrl(), endpoint.getCondition());
        return Future.failedFuture(new ServiceException(503, "Endpoint \"" + endpointName + "\" not accessible", new IllegalStateException(message)));
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
    String finalUrl = url;
    
    String query = definition.getQuery();
    if (!Strings.isNullOrEmpty(definition.getQueryTemplate())) {
      try {
        query = pipeline.renderTemplate(definition.getName() + ":query", definition.getQueryTemplate());
      } catch (Throwable ex) {
        logger.warn("Failed to render query template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    String finalQuery = query;
    
    String credentials[];
    try {
      credentials = processCredentials(endpoint, executor, requestContext);
    } catch (Throwable ex) {
      logger.warn("Failed to process credentials: ", ex);
      return Future.failedFuture(ex);
    }
    
    return vertx.executeBlocking(() -> {
      return runInitializationInWorker(finalUrl, credentials, finalQuery, pipeline, requestContext);
    });
  }

  @SuppressFBWarnings(value = {"OBL_UNSATISFIED_OBLIGATION", "ODR_OPEN_DATABASE_RESOURCE", "SQL_INJECTION_JDBC"}, justification = "JDBC objects must be closed by JdbcReadStream")
  private ReadStreamWithTypes runInitializationInWorker(String finalUrl, String[] credentials, String finalQuery, PipelineInstance pipeline, RequestContext requestContext) throws RuntimeException {
    Connection connection;
    try {
      connection = DriverManager.getConnection(finalUrl, credentials[0], credentials[1]);
    } catch (Throwable ex) {
      throw new RuntimeException("Failed to establish connection to JDBC URL (" + finalUrl + "): ", ex);
    }
    
    ResultSet rs;
    Types types;
    
    try {
      AbstractSqlPreparer preparer = new JdbcSqlPreparer(connection);
      AbstractSqlPreparer.QueryAndArgs queryAndArgs = preparer.prepareSqlStatement(finalQuery, definition.getReplaceDoubleQuotes(), pipeline.getArgumentInstances());
      String sql = queryAndArgs.query();
      
      PreparedStatement statement;
      try {
        statement = connection.prepareStatement(sql);
        setFetchSize(finalUrl, statement);
        statement.setFetchDirection(ResultSet.FETCH_FORWARD);
      } catch (Throwable ex) {
        throw new RuntimeException("Failed to prepare statement for (" + sql + "): ", ex);
      }
      
      if (queryAndArgs.args() != null) {
        for (int i = 0; i < queryAndArgs.args().size(); ++i) {
          statement.setObject(i + 1, queryAndArgs.args().get(i));
        }
      }
      
      rs = statement.executeQuery();
      types = new Types();
      ResultSetMetaData rsmeta = rs.getMetaData();
      for (int i = 0; i < rsmeta.getColumnCount(); ++i) {
        String name = rsmeta.getColumnLabel(i + 1);
        int jdbcType = rsmeta.getColumnType(i + 1);
        
        DataType type = DataType.fromJdbcType(JDBCType.valueOf(jdbcType));
        
        if (definition.getColumnTypeOverrideMap() != null) {
          Map<String, DataType> ctomap = definition.getColumnTypeOverrideMap();
          if (ctomap.containsKey(name)) {
            type = ctomap.get(name);
          }
        }
        types.putIfAbsent(name, type);
      }
    } catch (Throwable ex) {
      try {
        connection.close();
      } catch (Throwable ex2) {
        logger.warn("Exception closing connection: ", ex2);
      }
      throw new RuntimeException("Failed to establish connection to JDBC URL (" + finalUrl + "): ", ex);
    }
    
    jdbcReadStream = new JdbcReadStream(this, this.context, types, connection, rs, definition.getProcessingBatchSize());
    
    jdbcReadStream.start(requestContext.getRequestId() + ":" + getName());
    jdbcReadStream.exceptionHandler(ex -> {
      addNameToContextLocalData();                
      logger.error("Exception occured in stream: ", ex);
    });
    return new ReadStreamWithTypes(jdbcReadStream, types);
  }

  private void setFetchSize(String finalUrl, PreparedStatement statement) throws SQLException {
    if (definition.getJdbcFetchSize() < 0) {
      if (finalUrl.startsWith("jdbc:mysql:")) {
        statement.setFetchSize(Integer.MIN_VALUE);
      } else if (finalUrl.startsWith("jdbc:sqlserver:")) {
        statement.setFetchSize(1000);
      } else {
        statement.setFetchSize(1000);
      }
    } else {
      statement.setFetchSize(definition.getJdbcFetchSize());
    }
  }
  
  static String coalesce(String one, String two) {
    if (one == null) {
      return two;
    } else {
      return one;
    }
  }

  private String[] processCredentials(Endpoint endpoint, PipelineExecutor executor, RequestContext requestContext) throws ServiceException {
    String username = null;
    String password = null;
    if (Strings.isNullOrEmpty(endpoint.getSecret())) {
      if (!Strings.isNullOrEmpty(endpoint.getUsername())) {
        username = endpoint.getUsername();
      }
      if (!Strings.isNullOrEmpty(endpoint.getPassword())) {
        password = endpoint.getPassword();
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
        username = credentials.getUsername();
      }
      if (!Strings.isNullOrEmpty(credentials.getPassword())) {
        password = credentials.getPassword();
      }
    }
    return new String[]{username, password};
  }
  
}
