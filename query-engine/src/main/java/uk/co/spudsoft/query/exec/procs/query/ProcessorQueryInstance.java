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
package uk.co.spudsoft.query.exec.procs.query;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorQuery;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to filter rows with an <a href="https://github.com/jirutka/rsql-parser">RSQL</a> (FIQL) expression.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.ProcessorQuery} that has a single RSQL expression.
 * <P>
 * The RSQL expression is evaluated against each row in the stream and only those that pass the evaluation remain in the stream.
 * <P>
 * The query processor is only useful when the stream already contains more rows than are required - thus, whilst the processor itself is not
 * particularly inefficient it is best not used if at all possible.
 * This generally means that the only valid use for the query processor is via that {@link uk.co.spudsoft.query.exec.filters.QueryFilter}, and even then
 * a templated SQL statement is likely to be a better alternative.
 * 
 * @author jtalbut
 */
public class ProcessorQueryInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorQueryInstance.class);
  
  /**
   * The single instance of {@link RSQLParser} that is required.
   */
  public static final RSQLParser RSQL_PARSER = new RSQLParser();
  
  private final String name;
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorQuery definition;
  private FilteringStream<DataRow> stream;
  
  private final String expression;
  private Types types;
  
  private Node rootNode;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorQueryInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorQuery definition, String name) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    this.name = name;
    this.expression = definition.getExpression();
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Process the RSQL/FIQL for a DataRow and return the result.
   * @param rootNode the root of the RSQL abstract tree.
   * @param row the {@link DataRow} being evaluated.
   * @return the value calculated by the RSQL evaluation.
   */
  static boolean evaluate(Node rootNode, DataRow row) {
    return rootNode.accept(new RsqlEvaluator(), row);
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    try {
      rootNode = RSQL_PARSER.parse(expression);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    this.stream = new FilteringStream<>(input.getStream(), (data) -> {
      return evaluate(rootNode, data);
    });
    this.types = input.getTypes();
    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
