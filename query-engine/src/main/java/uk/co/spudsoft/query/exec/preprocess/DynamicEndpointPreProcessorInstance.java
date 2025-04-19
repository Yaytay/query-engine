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
package uk.co.spudsoft.query.exec.preprocess;

import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.defn.DynamicEndpoint;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.EndpointType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.PreProcessorInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.web.RequestContextHandler;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * The Query Engine supports {@link uk.co.spudsoft.query.exec.PreProcessorInstance} that can modify the executing pipeline before it gets going.
 * <P>
 * This is the only PreProcessorInstance at this time.
 * <P>
 * See {@link uk.co.spudsoft.query.defn.DynamicEndpoint} for details of the configuration.
 * 
 * @author jtalbut
 */
public class DynamicEndpointPreProcessorInstance implements PreProcessorInstance {

  private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointPreProcessorInstance.class);

  private final Vertx vertx;
  private final Context context;
  private final DynamicEndpoint definition;
  private final String name;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param index zero based index of this pre-processor within the list of pre-processors in the pipeline.
   */
  public DynamicEndpointPreProcessorInstance(Vertx vertx, Context context, DynamicEndpoint definition, int index) {
    this.vertx = vertx;
    this.context = context;
    this.definition = definition;
    if (Strings.isNullOrEmpty(definition.getName())) {
      this.name = "PP" + index + "-" + definition.getClass().getSimpleName();
    } else {
      this.name = definition.getName();
    }
  }

  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    logger.debug("initialize()");
    SourceInstance sourceInstance = definition.getInput().getSource().createInstance(vertx, context, executor, "Dynamic Endpoint Source");
    FormatCaptureInstance format = new FormatCaptureInstance();
    PipelineInstance dePipeline = new PipelineInstance(
            pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, definition.getInput(), null, name)
            , format
    );
    return executor.initializePipeline(dePipeline)
            .onFailure(ex -> {
              logger.error("Dynamic pipeline initialization failed: ", ex);
            })
            .compose(v -> {
              logger.debug("de pipeline initialized, getting future");
              return ReadStreamToList.capture(format.getReadStream().getStream());
            })
            .compose(rows -> {
              logger.debug("de pipeline completed, processing {} rows", rows.size());
              try {
                for (DataRow row : rows) {
                  processEndpoint(pipeline, row);
                }
              } catch (IllegalArgumentException ex) {
                return Future.failedFuture(new ServiceException(500, "Unable to process DynamicEncpoint: " + ex.getMessage()));
              }
              logger.debug("de pipeline completed");
              return Future.succeededFuture();
            });
  }
  
  private void processEndpoint(PipelineInstance pipeline, DataRow data) {
    
    logger.debug("Processing dynamic endpoint: {}", data);
    String type = getField(data, definition.getTypeField());    
    if (type == null) {
      type = EndpointType.SQL.name();
    }
    String key = getField(data, definition.getKeyField());
    if (Strings.isNullOrEmpty(key)) {
      key = definition.getKey();
    }
    if (Strings.isNullOrEmpty(key)) {
      if (data.containsKey(definition.getKeyField())) {
        throw new IllegalArgumentException("No field with the name " + definition.getKeyField() + " was found in the resultset for the dynamic endpoint");
      } else {
        throw new IllegalArgumentException("No key specified for dynamic endpoint");
      }
    }
    String url = getField(data, definition.getUrlField());
    String urlTemplate = getField(data, definition.getUrlTemplateField());
    String username = getField(data, definition.getUsernameField());
    String password = getField(data, definition.getPasswordField());
    String secret = getField(data, definition.getSecretField());
    String condition = getField(data, definition.getConditionField());

    EndpointType endpointType;
    try {
      endpointType = EndpointType.valueOf(type);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("The type \"" + type + "\" is not one of the recognised values " + Arrays.toString(EndpointType.values()), ex);
    }

    Endpoint endpoint = Endpoint.builder()
            .condition(condition == null ? null : new Condition(condition))
            .password(password)
            .secret(secret)
            .type(endpointType)
            .url(url)
            .urlTemplate(urlTemplate)
            .username(username)
            .build();
        
    if (!Strings.isNullOrEmpty(condition)) {
      RequestContext requestContext = RequestContextHandler.getRequestContext(context);
      ConditionInstance cond = new ConditionInstance(condition);
      if (cond.evaluate(requestContext, data)) {
        pipeline.getSourceEndpoints().put(key, endpoint);      
      } else {
        logger.debug("Endpoint {} ({}) rejected by condition ({})", key, url, condition);
      }
    } else {
      pipeline.getSourceEndpoints().put(key, endpoint);      
    }
    
  }
  
  static String getField(DataRow row, String field) {
    if (Strings.isNullOrEmpty(field)) {
      return null;
    }
    Object value = row.get(field);
    if (value == null) {
      return null;
    } else if (value instanceof String s) {
      return s;
    } else {
      return value.toString();
    }
  }
  
}
