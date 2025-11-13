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
package uk.co.spudsoft.query.exec.sources.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.sources.sql.AbstractSqlPreparer;
import uk.co.spudsoft.query.logging.Log;

/**
 * SQL preparer for MySQL.
 * 
 * Be warned that the connection is retained by the object and may be used by getQuoteCharacter.
 * The connection will only be used once as long as (@link ResultSetMetaData#
 *
 * @author jtalbut
 */
public class JdbcSqlPreparer extends AbstractSqlPreparer {

  private static final Logger logger = LoggerFactory.getLogger(JdbcSqlPreparer.class);
  
  private final Connection conn;
  private final Log log;
  private String quoteCharacter;
  
  
  /**
   * Constructor.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param conn A live connection to a DataSource, required to determine the quote character.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "")
  public JdbcSqlPreparer(PipelineContext pipelineContext, Connection conn) {
    super(pipelineContext);
    this.log = new Log(logger, pipelineContext);
    this.conn = conn;
  }
  
  @Override
  protected void generateParameterNumber(StringBuilder builder, int number) {
    builder.append("?");
  }

  @Override
  protected boolean hasNumberedParameters() {
    return false;
  }

  @Override
  protected String getQuoteCharacter() {
    if (quoteCharacter == null) {
      try {
        DatabaseMetaData metadata = conn.getMetaData();
        quoteCharacter = metadata.getIdentifierQuoteString();
      } catch (Throwable ex) {
        log.warn().log("Failed to capture identifier quote string: ", ex);
      }
    }
    return quoteCharacter;
  }
  
}
