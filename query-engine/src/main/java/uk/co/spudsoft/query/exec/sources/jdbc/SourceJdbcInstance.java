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
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.SourceJdbc;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.sources.AbstractSource;
import uk.co.spudsoft.query.exec.sources.sql.SourceSqlStreamingInstance;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.ProtectedCredentials;
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

  private final SourceJdbc definition;

  private JdbcReadStream jdbcReadStream;

  private final Log log;
  
  
  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param definition The {@link SourceSql} definition.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The requestContext should not be modified by this class")
  public SourceJdbcInstance(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, SourceJdbc definition) {
    super(vertx, meterRegistry, pipelineContext);
    this.definition = definition;
    this.log = new Log(logger, pipelineContext);
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline) {

    RequestContext requestContext = pipelineContext.getRequestContext();

    String endpointName = definition.getEndpoint();
    if (Strings.isNullOrEmpty(endpointName)) {
      try {
        endpointName = pipeline.renderTemplate(definition.getName() + ":endpoint", definition.getEndpointTemplate());
      } catch (Throwable ex) {
        log.warn().log("Failed to render endpoint template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    Endpoint endpoint = pipeline.getSourceEndpoints().get(endpointName);
    if (endpoint == null) {
      return Future.failedFuture(new ServiceException(400, "Endpoint \"" + endpointName + "\" not found in " + pipeline.getSourceEndpoints().keySet()));
    }
    if (!JexlEvaluator.isNullOrBlank(endpoint.getCondition())) {
      ConditionInstance cond = endpoint.getCondition().createInstance();
      if (!cond.evaluate(pipelineContext, null)) {
        String message = String.format("Endpoint %s (%s) rejected by condition (%s)", endpointName, endpoint.getUrl(), endpoint.getCondition());
        log.warn().log("Endpoint {} ({}) rejected by condition ({})", endpointName, endpoint.getUrl(), endpoint.getCondition());
        return Future.failedFuture(new ServiceException(503, "Endpoint \"" + endpointName + "\" not accessible", new IllegalStateException(message)));
      }
    }

    String url = endpoint.getUrl();
    if (Strings.isNullOrEmpty(url)) {
      try {
        url = pipeline.renderTemplate(definition.getName() + ":url", endpoint.getUrlTemplate());
      } catch (Throwable ex) {
        log.warn().log("Failed to render url template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    String finalUrl = url;

    String query = definition.getQuery();
    if (!Strings.isNullOrEmpty(definition.getQueryTemplate())) {
      try {
        query = pipeline.renderTemplate(definition.getName() + ":query", definition.getQueryTemplate());
      } catch (Throwable ex) {
        log.warn().log("Failed to render query template ({}): ", definition.getEndpointTemplate(), ex);
        return Future.failedFuture(ex);
      }
    }
    String finalQuery = query;

    String credentials[];
    try {
      credentials = processCredentials(endpoint, executor, requestContext);
    } catch (Throwable ex) {
      log.warn().log("Failed to process credentials: ", ex);
      return Future.failedFuture(ex);
    }

    return runInitialization(finalUrl, credentials, finalQuery, pipeline);
  }

  @SuppressFBWarnings(value = {"OBL_UNSATISFIED_OBLIGATION", "ODR_OPEN_DATABASE_RESOURCE", "SQL_INJECTION_JDBC"}, justification = "JDBC objects must be closed by JdbcReadStream")
  private Future<ReadStreamWithTypes> runInitialization(String finalUrl, String[] credentials, String finalQuery, PipelineInstance pipeline) throws RuntimeException {

    Promise<ReadStreamWithTypes> result = Promise.promise();

    jdbcReadStream = new JdbcReadStream(Vertx.currentContext(), pipelineContext, definition, result);
    jdbcReadStream.exceptionHandler(ex -> {
      log.error().log("Exception occured in stream: ", ex);
    });

    jdbcReadStream.start(pipelineContext.getRequestContext().getRequestId() + " " + pipelineContext.getPipe(), finalUrl, credentials, finalQuery, pipeline);

    return result.future();
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
        log.warn().log(message);
        throw new ServiceException(503, "Endpoint \"" + definition.getEndpoint() + "\" not accessible", new IllegalStateException(message));
      }
      if (!JexlEvaluator.isNullOrBlank(credentials.getCondition())) {
        ConditionInstance cond = credentials.getCondition().createInstance();
        if (!cond.evaluate(pipelineContext, null)) {
          String message = String.format("Endpoint %s (%s) prevented from accessing secret %s by condition (%s)", definition.getEndpoint(), coalesce(endpoint.getUrl(), endpoint.getUrlTemplate()), endpoint.getSecret(), endpoint.getCondition());
          log.warn().log(message);
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
