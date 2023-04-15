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
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
public class DynamicEndpointPreProcessorInstance implements PreProcessorInstance {

  private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointPreProcessorInstance.class);

  private final Vertx vertx;
  private final Context context;
  private final DynamicEndpoint definition;
  
  public DynamicEndpointPreProcessorInstance(Vertx vertx, Context context, DynamicEndpoint definition) {
    this.vertx = vertx;
    this.context = context;
    this.definition = definition;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    logger.debug("initialize()");
    SourceInstance sourceInstance = definition.getInput().getSource().createInstance(vertx, context, executor, "Dynamic Endpoint Source");
    FormatCaptureInstance format = new FormatCaptureInstance();
    PipelineInstance dePipeline = new PipelineInstance(
            pipeline.getArguments()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, definition.getInput())
            , format
    );
    return executor.initializePipeline(dePipeline)
            .onFailure(ex -> {
              logger.error("Dynamic pipeline initialization failed: ", ex);
            })
            .compose(v -> {
              logger.debug("de pipeline initialized, getting future");
              return format.getFuture();
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
      throw new IllegalArgumentException("No key specified for dynamic endpoint");
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
      RequestContext requestContext = context.getLocal("ctx");
      ConditionInstance cond = new ConditionInstance(condition);
      if (cond.evaluate(requestContext)) {
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