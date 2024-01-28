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
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorQuery;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.exec.procs.PassthroughStream;

/**
 *
 * @author jtalbut
 */
public class ProcessorQueryInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorQueryInstance.class);
  
  public static final RSQLParser RSQL_PARSER = new RSQLParser();
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorQuery definition;
  private final PassthroughStream stream;
  
  private final String expression;
  
  private Node rootNode;
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorQueryInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorQuery definition) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    this.stream = new PassthroughStream(sourceNameTracker, this::passthroughStreamProcessor, context);
    this.expression = definition.getExpression();
  }

  private Future<Void> passthroughStreamProcessor(DataRow data, AsyncHandler<DataRow> chain) {
    sourceNameTracker.addNameToContextLocalData(context);
    try {
      logger.trace("process {}", data);
      
      if (evaluate(rootNode, data)) {
        return chain.handle(data);
      } else {
        return Future.succeededFuture();
      }

    } catch (Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  static boolean evaluate(Node rootNode, DataRow row) {
    return rootNode.accept(new RsqlEvaluator(), row);
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
    try {
      rootNode = RSQL_PARSER.parse(expression);
      return Future.succeededFuture();
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }

  @Override
  public DataRowStream<DataRow> getReadStream() {
    return stream.readStream();
  }

  @Override
  public WriteStream<DataRow> getWriteStream() {
    return stream.writeStream();
  }  
  
}
