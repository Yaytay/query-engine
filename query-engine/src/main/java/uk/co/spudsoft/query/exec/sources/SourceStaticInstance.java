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
package uk.co.spudsoft.query.exec.sources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ColumnType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.defn.SourceStatic;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 * {@link uk.co.spudsoft.query.exec.SourceInstance} class for generating a fixed, defined, stream.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.SourceStatic} object.
 * <P>
 * The stream will contain precisely the data from the definition.
 * <P>
 * This class should be in a package called uk.co.spudsoft.query.exec.sources.static, but that is not a valid package name.
 *
 * @author jtalbut
 */
public class SourceStaticInstance extends AbstractSource {

  private static final Logger logger = LoggerFactory.getLogger(SourceStaticInstance.class);

  private final String name;
  private final SourceStatic definition;
  private final Types types;
  private final Context context;
  
  
  /**
   * Constructor.
   * @param vertx The vertx instance.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param meterRegistry MeterRegistry for production of processor-specific metrics.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition The configuration of this source.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The requestContext should not be modified by this class")
  public SourceStaticInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, SourceStatic definition) {
    super(vertx, meterRegistry, auditor, pipelineContext);
    this.name = definition.getName();
    this.types = new Types();
    this.definition = definition;
    List<ColumnType> inputTypes = definition.getTypes();
    inputTypes.forEach(columnType -> {
      types.putIfAbsent(columnType.getColumn(), columnType.getType());
    });
    this.context = vertx.getOrCreateContext();
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    List<DataRow> rows = new ArrayList<>(definition.getRows().size());
    List<ColumnType> inputTypes = definition.getTypes();
    List<List<Object>> inputRows = definition.getRows();
    try {
      for (int rowIdx = 0; rowIdx < inputRows.size(); ++rowIdx) {
        List<Object> inputRow = inputRows.get(rowIdx);
        DataRow outputRow = DataRow.create(types);
        for (int i = 0; i < inputTypes.size(); ++i) {
          ColumnType columnType = inputTypes.get(i);
          outputRow.put(columnType.getColumn(), columnType.getType().cast(pipelineContext, inputRow.get(i)));
        }
        rows.add(outputRow);
      }
    } catch (Throwable ex) {
      return Future.failedFuture(new IllegalArgumentException("Unable to construct DataRows from input", ex));
    }
    
    ReadStreamWithTypes result = new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, context, rows), types);
    return Future.succeededFuture(result);
  }
  
}

