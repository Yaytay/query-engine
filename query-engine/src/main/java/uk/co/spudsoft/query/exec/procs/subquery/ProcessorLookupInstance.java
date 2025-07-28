/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.procs.subquery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.tsegismont.streamutils.impl.MappingStream;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorLookup;
import uk.co.spudsoft.query.defn.ProcessorLookupField;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to create field values from a map loaded during initialization.
 * <P>
 * It will be rare for this processor to do a better job than a database server, but it can be worth trying if a query is slow
 * because of reference data joins.
 * Typically this is worth consideration when there is a large query with multiple joins to a lookup table per row.
 * <P>
 * The entire lookup map will be loaded into a {@link java.util.HashMap}, so beware of memory limits.
 *
 * @author jtalbut
 */
public class ProcessorLookupInstance implements ProcessorInstance {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorLookupInstance.class);
  
  private final Vertx vertx;
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final MeterRegistry meterRegistry;
  private final String name;

  private final ProcessorLookup definition;
  private final Map<Comparable<?>, Comparable<?>> map = new HashMap<>();
  
  private DataType outputFieldType;
  private ReadStream<DataRow> stream;
  
  private final Set<String> includedFields = new HashSet<>();
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorLookupInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, MeterRegistry meterRegistry, ProcessorLookup definition, String name) {
    this.vertx = vertx;
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.meterRegistry = meterRegistry;
    this.name = name;
    this.definition = definition;
  }

  @Override
  public String getName() {
    return name;
  }
  
  private record KVP(Comparable<?> key, Comparable<?> value) {}

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    SourceInstance sourceInstance = definition.getMap().getSource().createInstance(vertx, context, meterRegistry, executor, getName() + ".map");
    FormatCaptureInstance fieldDefnStreamCapture = new FormatCaptureInstance();
    PipelineInstance childPipeline = new PipelineInstance(
            pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, definition.getMap(), null, getName())
            , fieldDefnStreamCapture
    );
    
    long start = System.currentTimeMillis();
    
    for (ProcessorLookupField field : definition.getLookupFields()) {
      if (field.getCondition() == null) {
        includedFields.add(field.getKeyField());
      } else {
        ConditionInstance cond = field.getCondition().createInstance();
        if (cond.evaluate(context.get("req"), null)) {
          includedFields.add(field.getKeyField());          
        } else {
          logger.info("Field {} excluded by condition {}", field.getKeyField(), field.getCondition().getExpression());
        }
      }
    }
    
    return executor.initializePipeline(childPipeline)
            .compose(v -> {
              return ReadStreamToList.map(
                      fieldDefnStreamCapture.getReadStream().getStream()
                      , row -> {
                        if (row.isEmpty()) {
                          return null;
                        } else {
                          if (outputFieldType == null) {
                            outputFieldType = row.getType(definition.getLookupValueField());
                          }
                          return new KVP(
                                  row.get(definition.getLookupKeyField())
                                  , row.get(definition.getLookupValueField())
                          );
                        }
                      });
            })
            .compose(collated -> {
              for (KVP kvp : collated) {
                map.put(kvp.key, kvp.value);
              }
              logger.info("{} Loaded {} mappings in {}s", getName(), map.size(), ((System.currentTimeMillis() - start) / 1000.0));
              for (ProcessorLookupField field : definition.getLookupFields()) {
                if (includedFields.contains(field.getKeyField())) {
                  input.getTypes().putIfAbsent(field.getValueField(), outputFieldType);
                }
              }
              stream = new MappingStream<>(input.getStream(), this::runProcess);

              return Future.succeededFuture(new ReadStreamWithTypes(stream, input.getTypes()));
            });
  }
  
  private DataRow runProcess(DataRow input) {
    for (ProcessorLookupField field : definition.getLookupFields()) {
      Comparable<?> key = input.get(field.getKeyField());
      if (key != null && includedFields.contains(field.getKeyField())) {
        Comparable<?> value = map.get(key);
        if (value != null) {
          input.put(field.getValueField(), value);
        }
      }
    }
    return input;
  }
  
}
