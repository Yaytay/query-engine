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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.defn.DynamicEndpoint;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.EndpointType;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.PreProcessorInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
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
  private final MeterRegistry meterRegistry;
  private final PipelineContext pipelineContext;
  private final DynamicEndpoint definition;
  private final String name;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param definition the definition of this processor.
   * @param index zero based index of this pre-processor within the list of pre-processors in the pipeline.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MeterRegistry is designed to be modified")
  public DynamicEndpointPreProcessorInstance(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, DynamicEndpoint definition, int index) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.pipelineContext = pipelineContext;
    this.definition = definition;
    this.name = Strings.isNullOrEmpty(definition.getName()) ? "dynamicEndpoints[" + index + "]" : definition.getName();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    logger.debug("initialize()");

    String childName = pipeline.getPipelineContext().getPipe() + "." + name + ".input";
    PipelineContext childContext = pipeline.getPipelineContext().child(childName);

    SourceInstance sourceInstance = definition.getInput().getSource().createInstance(vertx, childContext, meterRegistry, executor);
    FormatCaptureInstance format = new FormatCaptureInstance();
    PipelineInstance dePipeline = new PipelineInstance(
            childContext
            , pipeline.getDefinition()
            , pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, childContext, definition.getInput(), null)
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
                for (int i = 0; i < rows.size(); ++i) {
                  processEndpoint(pipeline, i, rows.get(i));
                }
              } catch (IllegalArgumentException ex) {
                return Future.failedFuture(new ServiceException(500, "Unable to process DynamicEndpoint: " + ex.getMessage()));
              }
              logger.debug("de pipeline completed");
              return Future.succeededFuture();
            });
  }

  @Override
  public String getName() {
    return name;
  }
  
  private void processEndpoint(PipelineInstance pipeline, int idx, DataRow data) {
    
    String type = getField(data, definition.getTypeField());    
    if (type == null) {
      type = EndpointType.SQL.name();
    }
    String key = getField(data, definition.getKeyField());
    if (Strings.isNullOrEmpty(key)) {
      key = definition.getKey();
    }
    logger.debug("Processing dynamic endpoint {}: {} ({})", idx, key, type);
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
      RequestContext requestContext = pipelineContext.getRequestContext();
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
