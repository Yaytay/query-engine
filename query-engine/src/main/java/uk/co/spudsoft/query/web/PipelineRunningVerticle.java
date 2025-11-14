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
package uk.co.spudsoft.query.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Vert.x Verticle for running Query Engine Pipelines.
 * 
 * @author jtalbut
 */
public class PipelineRunningVerticle extends VerticleBase {
  
  private static final Logger logger = LoggerFactory.getLogger(PipelineRunningVerticle.class);
  
  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final Auditor auditor;
  private final PipelineExecutor pipelineExecutor;
  
  private String threadName;
  
  /**
   * Constructor.
   * @param vertx The Vertx instance.
   * @param meterRegistry The Prometheus MeterRegistry.
   * @param auditor Tracking object.
   * @param pipelineExecutor The Pipeline Executor.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Both auditor and meterRegistry are appropriately safe for the calls made to them")
  public PipelineRunningVerticle(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineExecutor pipelineExecutor) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.auditor = auditor;
    this.pipelineExecutor = pipelineExecutor;
  }

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context); 
    this.threadName = Thread.currentThread().getName();
  }

  /**
   * Get the name of the thread that this Verticle is bound to.
   * @return the name of the thread that this Verticle is bound to.
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * Handler a PipelineRunningTask.
   * 
   * The task will be runOnContext.
   * 
   * @param task details of the pipeline to run.
   * @return a Future that will be completed when the Pipeline completes.
   */
  public Future<Void> handleRequest(PipelineRunningTask task) {
    Promise<Void> promise = Promise.promise();
    Context thisContext = Vertx.currentContext();    
    this.context.runOnContext(v -> { 
      handleRequestOnContext(task)
              .onComplete(ar -> {
                thisContext.runOnContext(v2 -> {
                  if (ar.succeeded()) {
                    promise.complete(ar.result());
                  } else {
                    promise.fail(ar.cause());
                  }
                });
              });
    });
    return promise.future();
  }
  
  private Future<Void> handleRequestOnContext(PipelineRunningTask task) {
    Log.decorate(logger.atInfo(), task.requestContext)
            .log("Creating PipelineInstance");
    try {
      PipelineInstance instance;
      PipelineContext rootContext = new PipelineContext("$", task.requestContext);
      FormatInstance formatInstance = task.chosenFormat.createInstance(vertx, rootContext, task.responseStream);
      SourceInstance sourceInstance = task.pipeline.getSource().createInstance(vertx, rootContext, meterRegistry, pipelineExecutor);
      instance = new PipelineInstance(
              rootContext
              , task.pipeline
              , task.arguments
              , task.pipeline.getSourceEndpointsMap()
              , pipelineExecutor.createPreProcessors(vertx, rootContext, task.pipeline)
              , sourceInstance
              , pipelineExecutor.createProcessors(vertx, rootContext, task.pipeline, task.queryStringParams)
              , formatInstance
      );
      Log.decorate(logger.atDebug(), rootContext)
              .addArgument(instance)
              .log("PipelineInstance: {}")
              ;
      return pipelineExecutor.initializePipeline(rootContext, instance).map(v -> instance)
              .compose(i -> {
                Log.decorate(logger.atInfo(), rootContext)
                        .log("Pipeline initiated")
                        ;
                return instance.getFinalPromise().future();
              });
    } catch (Throwable ex) {
      Log.decorate(logger.atWarn(), task.requestContext).log("Failed to run pipeline: ", ex);
      return Future.failedFuture(ex);
    }
  }
}
