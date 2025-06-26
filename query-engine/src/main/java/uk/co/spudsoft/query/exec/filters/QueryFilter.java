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
package uk.co.spudsoft.query.exec.filters;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorQuery;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.query.ProcessorQueryInstance;

/**
 * Filter for converting _query command line arguments into {@link uk.co.spudsoft.query.exec.procs.query.ProcessorQueryInstance}s.
 * <P>
 * The value of the argument must be a valid RQL expression.
 * If at all possible query instructions should be implemented in the query, rather than using this filter.
 * <P>
 * There are two extensions to the standard RQL processor:
 * <UL>
 * <LI>String comparisons may begin or end with a '*' character that will be treated as a wildcard.
 * <LI>There is a '=~' operator for performing regular expression matches.
 * </UL>
 * 
 * 
 * @author jtalbut
 */
public class QueryFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(QueryFilter.class);

  /**
   * Constructor.
   */
  public QueryFilter() {
  }
  
  @Override
  public String getKey() {
    return "_query";
  }
  
  @Override
  public ProcessorInstance createProcessor(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String argument, String name) throws IllegalArgumentException {
    try {
      argument = FiqlToRsqlConverter.convertFiqlToRsql(argument);
      ProcessorQueryInstance.RSQL_PARSER.parse(argument);
    } catch (Throwable ex) {
      logger.warn("Failed to parse argument to _query filter (\"{}\"): ", argument, ex);
      throw new IllegalArgumentException("Invalid argument to _query filter, should be a valid RSQL expression");
    }
    ProcessorQuery definition = ProcessorQuery.builder().expression(argument).build();
    return definition.createInstance(vertx, sourceNameTracker, context, name);
  }
  
}
