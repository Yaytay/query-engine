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
package uk.co.spudsoft.query.exec;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 * Public interface to execute a pipeline.
 * @author jtalbut
 */
public interface PipelineExecutor extends SharedMap {

  /**
   * Get a secret from the configured protected credentials.
   * <p>
   * See {@link uk.co.spudsoft.query.main.Parameters#setSecrets(java.util.Map)}.
   * 
   * @param name The name of the secret being sought.
   * @return The configured {@link ProtectedCredentials} instance, or null if not found.
   */
  ProtectedCredentials getSecret(String name);
  
  /**
   * Build a {@link Map} of argument names to {@link ArgumentInstance} objects with correctly typed {@link ArgumentInstance#values values}.
   * 
   * @param requestContext The request context, the calculated {@link Map} is stored in the request context.
   * @param definitions The {@link Argument} definitions from the {@link Pipeline} definition.
   * @param valuesMap The map of query string parameters received.
   * @return a {@link Map} of argument names to {@link ArgumentInstance} objects with correctly typed {@link ArgumentInstance#values values}.
   */
  Map<String, ArgumentInstance> prepareArguments(RequestContext requestContext, List<Argument> definitions, MultiMap valuesMap);
  
  /**
   * Validate a {@link Pipeline} definition.
   * 
   * @param definition The {@link Pipeline} on which {@link Pipeline#validate()} will be called.
   * @return A Future that will be completed with the unchanged Pipeline if it is valid, or an {@link IllegalArgumentException} if not.
   */
  Future<Pipeline> validatePipeline(Pipeline definition);
  
  /**
   * Create all of the {@link ProcessorInstance} objects specified in the {@link Pipeline} definition and {@link uk.co.spudsoft.query.exec.filters.Filter} arguments in the query string.
   * <p>
   * Note that correct behaviour of {@link uk.co.spudsoft.query.exec.filters.Filter} arguments is predicated upon MultiMap iteration being stable and in the order of the arguments.
   * @param vertx The Vert.x instance.
   * @param sourceNameTracker Helper to allow any {@link SourceInstance} objects to identify themselves to the {@link Context} for logging.
   * @param context The Vert.x {@link Context} for any asynchronous processing.
   * @param definition The definition of the {@link SourcePipeline pipeline}.
   * @param params The original parameters, for identifying {@link uk.co.spudsoft.query.exec.filters.Filter} arguments.
   * @param parentName The name of any parent Processor, used in generation of a default name for the processors.
   * @return all of the {@link ProcessorInstance} objects specified in the {@link Pipeline} definition and {@link uk.co.spudsoft.query.exec.filters.Filter} arguments in the query string.
   */
  List<ProcessorInstance> createProcessors(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, SourcePipeline definition, MultiMap params, String parentName);
  
  /**
   * Create any {@link PreProcessorInstance} objects specified in the {@link Pipeline} definition. 
   * @param vertx The Vert.x instance.
   * @param context The Vert.x {@link Context} for any asynchronous processing.
   * @param definition The definition of the {@link SourcePipeline pipeline}.
   * @return {@link List} of any {@link PreProcessorInstance} objects specified in the {@link Pipeline} definition. 
   */
  List<PreProcessorInstance> createPreProcessors(Vertx vertx, Context context, Pipeline definition);

  /**
   * Initialize the {@link PipelineInstance}.
   * <p>
   * This starts the whole process.
   * After this method has been called the only external progress notification will be when the returned {@link Future} completes
   * (or the {@link PipelineInstance#finalPromise} completes, which will happen immediately after).
   * 
   * @param pipeline the {@link PipelineInstance} to initialize.
   * @return A Future that will be completed when the pipeline completes.
   */
  Future<Void> initializePipeline(PipelineInstance pipeline);
  
  /**
   * Get the correct format to use given the prepared information from the request.
   * 
   * @param formats The {@link Format} specifications from the {@link Pipeline} definition.
   * @param requested The formats specified in the request
   * @return the correct format to use given the prepared information from the request.
   */
  Format getFormat(List<Format> formats, FormatRequest requested);

  /**
   * Report an event relating to the current run.
   * 
   * @param pipelineTitle The title of the pipeline.
   * @param sourceName The source that this message relates to - may be null.
   * @param processorName The processor that this message relates to - may be null.
   * @param count Any relevant count for the source/processor.
   * @param completed True if the entire pipeline has completed.
   * @param succeeded True if the entire pipeline was successful.
   * @param message The message, which may contain slf4j formatting instructions.
   * @param arguments Arguments to the message.
   */
  void progressNotification(String pipelineTitle
          , String sourceName
          , String processorName
          , Long count
          , boolean completed
          , Boolean succeeded
          , String message
          , Object... arguments
  );

}
