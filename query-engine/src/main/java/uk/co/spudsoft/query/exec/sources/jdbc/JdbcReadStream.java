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

import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.SourceJdbc;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.sources.sql.AbstractSqlPreparer;
import uk.co.spudsoft.query.logging.Log;

/**
 * Processor for converting a JDBC {@link ResultSet} into a Vert.x {@link ReadStream}.
 *
 * The logic will attempt to keep between fetchSize/2 and fetchSize rows buffered at all times (until the rows run out).
 *
 * @author jtalbut
 */
public class JdbcReadStream implements ReadStream<DataRow> {

  private static final Logger logger = LoggerFactory.getLogger(JdbcReadStream.class);

  private final Context context;
  private final SourceJdbc definition;
  private final int processingBatchSize;
  private final Promise<ReadStreamWithTypes> initPromise;

  private final PipelineContext pipelineContext;
  private final Log log;
  
  private Types types;

  private final Deque<DataRow> items = new ArrayDeque<>();

  private final ReentrantLock queueLock = new ReentrantLock();
  private final Condition notFull = queueLock.newCondition();

  private Handler<Throwable> exceptionHandler;
  private Handler<DataRow> handler;
  private Handler<Void> endHandler;

  private long demand;
  private volatile boolean initialized;
  private volatile boolean emitting;
  private volatile boolean ended;
  private volatile boolean completed;
  private long rowsOutput;

  /**
   * Constructor.
   * @param context The Vert.x context to use for asynchronous method calls.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition The source definition.
   * @param initPromise The promise to complete when the query starts returning data.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JdbcReadStream(Context context, PipelineContext pipelineContext, SourceJdbc definition, Promise<ReadStreamWithTypes> initPromise) {
    this.context = context;
    this.pipelineContext = pipelineContext;
    this.definition = definition;
    this.processingBatchSize = definition.getProcessingBatchSize();
    this.initPromise = initPromise;
    this.log = new Log(logger, pipelineContext);
  }

  /**
   * Start processing the {@link ResultSet} in a new thread.
   *
   * @param name The name to assign to the new thread.
   * @param dataSourceUrl The JDBC URL for the datasource.
   * @param credentials The credentials to use to connect to the data source.
   *    Two element array, the username and then the password.
   * @param sql The SQL statement to execute.
   * @param pipeline The {@link PipelineInstance} to use to obtain the arguments.
   */
  public void start(String name
          , String dataSourceUrl
          , String[] credentials
          , String sql
          , PipelineInstance pipeline
  ) {
    Thread thread = new Thread(() -> {

      runOnThread(dataSourceUrl, credentials, sql, pipeline);
    }, name);
    thread.start();
  }

  private void checkProcessing(String message) {
    if (initialized) {
      if (!emitting) {
        log.trace().log(message);
        emitting = true;
        context.runOnContext(v -> process());
      }
    } else {
      log.trace().log("Not initialized yet");
    }
  }

  @SuppressFBWarnings("SQL_INJECTION_JDBC")
  private void runOnThread(
          String dataSourceUrl
          , String[] credentials
          , String sql
          , PipelineInstance pipeline
  ) throws RuntimeException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;
    ResultSetMetaData rsmeta = null;

    long start = System.currentTimeMillis();

    try {
      try {
        log.debug().log("{}: Connecting to {}", (System.currentTimeMillis() - start) / 1000.0, dataSourceUrl);
        connection = DriverManager.getConnection(dataSourceUrl, credentials[0], credentials[1]);
      } catch (Throwable ex) {
        log.warn().log("{}: Failed to connect to {} for {}: ", (System.currentTimeMillis() - start) / 1000.0, dataSourceUrl, credentials[0], ex);
        context.runOnContext(v -> {
          initPromise.fail(new RuntimeException("Failed to establish connection to JDBC URL (" + dataSourceUrl + "): ", ex));
        });
        return ;
      }

      String preparedSql = null;
      try {
        AbstractSqlPreparer preparer = new JdbcSqlPreparer(pipelineContext, connection);
        AbstractSqlPreparer.QueryAndArgs queryAndArgs = preparer.prepareSqlStatement(sql, definition.getReplaceDoubleQuotes(), pipeline.getArgumentInstances());
        preparedSql = queryAndArgs.query();

        try {
          log.debug().log("{}: Preparing statement {}", (System.currentTimeMillis() - start) / 1000.0, preparedSql);
          statement = connection.prepareStatement(preparedSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
          setFetchSize(definition, dataSourceUrl, statement);
          statement.setFetchDirection(ResultSet.FETCH_FORWARD);
        } catch (Throwable ex) {
          throw new RuntimeException("Failed to prepare statement for (" + sql + "): ", ex);
        }

        if (queryAndArgs.args() != null) {
          for (int i = 0; i < queryAndArgs.args().size(); ++i) {
            statement.setObject(i + 1, queryAndArgs.args().get(i));
          }
        }
      } catch (Throwable ex) {
        log.warn().log("{}: Failed to prepare statement \"{}\": ", (System.currentTimeMillis() - start) / 1000.0, preparedSql, ex);
        context.runOnContext(v -> {
          initPromise.fail(new RuntimeException("Failed to prepare statement: ", ex));
        });
        return;
      }

      try {
        log.debug().log("{}: Executing query", (System.currentTimeMillis() - start) / 1000.0);
        rs = statement.executeQuery();
        types = new Types();
        log.debug().log("{}: Getting metadata", (System.currentTimeMillis() - start) / 1000.0);
        rsmeta = rs.getMetaData();
        for (int i = 0; i < rsmeta.getColumnCount(); ++i) {
          String name = rsmeta.getColumnLabel(i + 1);
          int jdbcType = rsmeta.getColumnType(i + 1);

          DataType type = DataType.fromJdbcType(pipelineContext, JDBCType.valueOf(jdbcType));

          if (definition.getColumnTypeOverrideMap() != null) {
            Map<String, DataType> ctomap = definition.getColumnTypeOverrideMap();
            if (ctomap.containsKey(name)) {
              type = ctomap.get(name);
            }
          }
          types.putIfAbsent(name, type);
        }
      } catch (Throwable ex) {
        log.warn().log("{}: Failed to execute statement: ", (System.currentTimeMillis() - start) / 1000.0, ex);
        context.runOnContext(v -> {
          initPromise.fail(new RuntimeException("Failed to execute statement: ", ex));
        });
        return;
      }

      context.runOnContext(v -> {
        initPromise.complete(new ReadStreamWithTypes(this, types));
      });

      log.debug().log("{}: Processing results", (System.currentTimeMillis() - start) / 1000.0);
      resultSetWalk(rs, rsmeta);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (Throwable ex) {
          log.warn().log("Exception closing PreparedStatement: ", ex);
        }
      }
      if (rs != null) {
        try {
          rs.close();
        } catch (Throwable ex) {
          log.warn().log("Exception closing ResultSet: ", ex);
        }
      }
      if (connection != null) {
        try {
          connection.close();
        } catch (Throwable ex) {
          log.warn().log("Exception closing Connection: ", ex);
        }
      }
    }
  }

  static void setFetchSize(SourceJdbc definition, String finalUrl, PreparedStatement statement) throws SQLException {
    if (definition.getJdbcFetchSize() < 0) {
      if (finalUrl.startsWith("jdbc:mysql:")) {
        statement.setFetchSize(Integer.MIN_VALUE);
      } else if (statement.isWrapperFor(SQLServerStatement.class)) {
        statement.unwrap(SQLServerStatement.class).setResponseBuffering("adaptive");
      } else {
        statement.setFetchSize(1000);
      }
    } else {
      statement.setFetchSize(definition.getJdbcFetchSize());
    }
  }

  @SuppressFBWarnings(value = "UL_UNRELEASED_LOCK_EXCEPTION_PATH", justification = "False positive, which is a shame because it's a useful test")
  private void resultSetWalk(ResultSet rs, ResultSetMetaData rsmeta) {

    long rows = 0;
    try {
      while (rs.next()) {
        DataRow row = dataRowFromResult(rsmeta, rs);
        if (report(rows)) {
          log.debug().log("Received {} rows", rows);
        }
        ++rows;

        log.trace().log("resultSetWalker: Taking out queue lock for results walker");
        queueLock.lock();
        try {
          items.add(row);
          while (items.size() > processingBatchSize) {
            log.trace().log("resultSetWalker: Waiting until notFull ({} > {})", items.size(), processingBatchSize);
            notFull.await();
            log.trace().log("resultSetWalker: notFull ({} > {})", items.size(), processingBatchSize);
          }
          checkProcessing("resultSetWalker: starting process");
        } finally {
          log.trace().log("resultSetWalker: Releasing queue lock from results walker");
          queueLock.unlock();
        }
      }
    } catch (Throwable ex) {
      log.error().log("resultSetWalker: Failed to process resultset: ", ex);
    } finally {
      log.trace().log("resultSetWalker: Finished iterating rows");
      complete();
    }
  }

  private DataRow dataRowFromResult(ResultSetMetaData rsmeta, ResultSet rs) throws SQLException {
    DataRow row = DataRow.create(types);
    for (int i = 0; i < rsmeta.getColumnCount(); ++i) {
      String name = rsmeta.getColumnLabel(i + 1);

      DataType type = types.get(name);
      Object value;
      if  (type == DataType.DateTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        value = rs.getTimestamp(i + 1, cal);
      } else {
        value = rs.getObject(i + 1);
      }
      try {
        Comparable<?> typedValue = type.cast(pipelineContext, value);
        row.put(name, typedValue);
      } catch (Throwable ex) {
        log.warn().log("Failed to cast {} ({}) value ({}): ", name, i, value, ex);
      }
    }
    return row;
  }

  /**
   * Mark that no more items will be added.
   * @return this, so that this method may be used in a fluent manner.
   */
  private JdbcReadStream complete() {
    queueLock.lock();
    try {
      this.completed = true;
    } finally {
      queueLock.unlock();
    }
    if (initialized) {
      log.trace().log("Completed with buffer of {}", items.size());
    }
    checkProcessing("Completed");
    return this;
  }

  static boolean report(long rows) {
    if (rows < 10000) {
      return false;
    } else if (rows < 1000000) {
      return rows % 10000 == 0;
    } else if (rows < 10000000) {
      return rows % 100000 == 0;
    } else {
      return rows % 1000000 == 0;
    }
  }

  @SuppressFBWarnings(value = "UL_UNRELEASED_LOCK_EXCEPTION_PATH", justification = "False positive, which is a shame because it's a useful test")
  private void process() {
    Handler<Void> endHandlerCaptured = null;
    log.trace().log("Starting to process {} {} ({}) {} {}", ended, emitting, completed, demand, items.size());
    long rows = 0;
    boolean done = false;
    while (!ended && emitting) {
      Handler<Throwable> exceptionHandlerCaptured;
      Handler<DataRow> handlerCaptured = null;
      DataRow item = null;

      log.trace().log("Taking out queue lock for stream processor");
      queueLock.lock();
      try {
        if (demand <= 0) {
          log.trace().log("Stop emitting, demand ({}) < 0", demand);
          emitting = false;
          break ;
        } else if (demand < Long.MAX_VALUE) {
          --demand;
        }
        exceptionHandlerCaptured = exceptionHandler;
        if (!items.isEmpty()) {
          item =  items.pop();
          rows = ++rowsOutput;
          handlerCaptured = handler;
          if (items.size() < processingBatchSize / 2) {
            log.trace().log("Signalling notFull");
            notFull.signal();
          } else {
            log.trace().log("Not signalling - buffer too full {} > {}", items.size(), processingBatchSize / 2);
          }
        } else {
          log.trace().log("Stop emitting, no items");
          emitting = false;
          done = completed;
          notFull.signal();
        }
      } finally {
        log.trace().log("Releasing queue lock from stream processor");
        queueLock.unlock();
      }
      if (item != null && handlerCaptured != null) {
        try {
          log.trace().log("Handling {}", item);
          handlerCaptured.handle(item);
        } catch (Throwable ex) {
          if (exceptionHandlerCaptured != null) {
            exceptionHandlerCaptured.handle(ex);
          } else {
            log.warn().log("Exception handling item in QueueReadStream: ", ex);
          }
        }
        if (report(rows)) {
          log.debug().log("Passed on {} rows", rows);
        }
        ++rows;
      }
    }
    if (done && !ended) {
      endHandlerCaptured = endHandler;
      if (endHandlerCaptured != null) {
        log.trace().log("Calling endHandler");
        endHandlerCaptured.handle(null);
      } else {
        log.trace().log("No endHandler");
      }
    }
  }

  @Override
  public JdbcReadStream exceptionHandler(Handler<Throwable> handler) {
    queueLock.lock();
    try {
      this.exceptionHandler = handler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream handler(Handler<DataRow> handler) {
    queueLock.lock();
    try {
      this.handler = handler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream endHandler(Handler<Void> endHandler) {
    queueLock.lock();
    try {
      this.endHandler = endHandler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream pause() {
    log.trace().log("pause()");
    queueLock.lock();
    try {
      demand = 0;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream resume() {
    log.trace().log("resume()");
    queueLock.lock();
    try {
      initialized = true;
      demand = Long.MAX_VALUE;
    } finally {
      queueLock.unlock();
    }
    checkProcessing("Resume");
    return this;
  }

  @Override
  public JdbcReadStream fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException("Negative fetch amount");
    }
    log.trace().log("fetch({})", amount);
    queueLock.lock();
    try {
      initialized = true;
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
    } finally {
      queueLock.unlock();
    }
    checkProcessing("Fetch");
    return this;
  }
}
