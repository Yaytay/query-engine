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
package uk.co.spudsoft.query.exec.fmts.json;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.sqlclient.impl.Utils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.web.RequestContextHandler;


/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates JSON output.
 * <P>
 * The JSON structure itself is a single array, with one object for each row.
 * 
 * @author jtalbut
 */
public final class FormatJsonInstance implements FormatInstance {
  
  private static final Logger logger = LoggerFactory.getLogger(FormatJsonInstance.class);
 
  private final WriteStream<Buffer> outputStream;
  private final FormatJson defn;
  private final FormattingWriteStream formattingStream;
  
  private static final Buffer OPEN_ARRAY = Buffer.buffer("[");
  private static final Buffer COMMA = Buffer.buffer(",");
  private static final Buffer CLOSE_ARRAY = Buffer.buffer("]");
  private static final Buffer CLOSE_ARRAY_AND_OBJECT = Buffer.buffer("]}");
  private final AtomicBoolean started = new AtomicBoolean();
  private Types types;
  private String title;
  private String description;
  
  private final DateTimeFormatter dateFormatter;
  private final DateTimeFormatter dateTimeFormatter;
  private final DateTimeFormatter timeFormatter;
  
  private boolean dateTimeAsEpochSeconds = false;
  private boolean dateTimeAsEpochMillis = false;
  
  /**
   * Constructor.
   * @param outputStream The WriteStream that the data is to be sent to.
   * @param defn The definition of the output.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatJsonInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatJsonInstance(WriteStream<Buffer> outputStream, FormatJson defn) {
    this.outputStream = outputStream;
    this.defn = defn;
    
    if (Strings.isNullOrEmpty(defn.getDateFormat())) {
      this.dateFormatter = null;
    } else {
      this.dateFormatter = DateTimeFormatter.ofPattern(defn.getDateFormat());
    }
    
    if (Strings.isNullOrEmpty(defn.getDateTimeFormat())) {
      this.dateTimeFormatter = null;
    } else if ("EPOCH_SECONDS".equals(defn.getDateTimeFormat())) {
      this.dateTimeFormatter = null;
      this.dateTimeAsEpochSeconds = true;
    } else if ("EPOCH_MILLISECONDS".equals(defn.getDateTimeFormat())) {
      this.dateTimeFormatter = null;
      this.dateTimeAsEpochMillis = true;
    } else {
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(defn.getDateTimeFormat());
    }
    
    if (Strings.isNullOrEmpty(defn.getTimeFormat())) {
      this.timeFormatter = null;
    } else {
      this.timeFormatter = DateTimeFormatter.ofPattern(defn.getTimeFormat());
    }
    
    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> {
              return start();
            }
            , row -> {
              if (row.isEmpty()) {
                return Future.succeededFuture();
              } else if (started.get()) {
                Buffer buffer = COMMA.copy().appendBuffer(toJson(row).toBuffer());
                return outputStream.write(buffer);
              } else {
                started.set(true);
                return outputStream.write(toJson(row).toBuffer());
              }
            }
            , rows -> {              
              Context vertxContext = Vertx.currentContext();
              if (vertxContext != null) {
                RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                if (requestContext != null) {
                  requestContext.setRowsWritten(rows);
                }
              }
              return end()
                      .compose(v2 -> outputStream.end())
                      ;
            }
    );
  }
  
  JsonObject toJson(DataRow row) {
    JsonObject json = new JsonObject();
    row.forEach((cd, value) -> {
      switch (cd.type()) {
        case Date:
          if (dateFormatter == null) {
            json.put(cd.name(), Utils.toJson(value));
          } else {
            if (value instanceof TemporalAccessor ta) {
              json.put(cd.name(), dateFormatter.format(ta));
            }
          }
          break ;
          
        case DateTime:
          if (value == null) {
            json.put(cd.name(), null);
          } else if (value instanceof LocalDateTime ldt) {
            if (dateTimeAsEpochMillis) {
              json.put(cd.name(), (Long) ldt.toInstant(ZoneOffset.UTC).toEpochMilli());
            } else if (dateTimeAsEpochSeconds) {
              json.put(cd.name(), ldt.toEpochSecond(ZoneOffset.UTC));
            } else if (dateTimeFormatter == null) {
              json.put(cd.name(), Utils.toJson(ldt.toInstant(ZoneOffset.UTC)));
            } else {
              json.put(cd.name(), dateTimeFormatter.format(ldt));
            }
          } else {
            logger.warn("DateTime value is not LocalDateTime (it's {} of {})", value.getClass(), value);
            json.put(cd.name(), Utils.toJson(value));
          }
          break ;
          
        case Time:
          if (timeFormatter == null) {
            json.put(cd.name(), Utils.toJson(value));
          } else {
            if (value instanceof TemporalAccessor ta) {
              json.put(cd.name(), timeFormatter.format(ta));
            }
          }
          break ;
        default:
          json.put(cd.name(), Utils.toJson(value));
          break;
      }
    });
    return json;
  }
  
  private Future<Void> start() {
    if (Strings.isNullOrEmpty(defn.getDataName())) {
      return outputStream.write(OPEN_ARRAY);
    } else {
      String start;
      if (Strings.isNullOrEmpty(defn.getMetadataName())) {
        start = "{\"" + defn.getDataName() + "\":";
      } else {
        logger.debug("Data types: {}", types);
        Types metaTypes = new Types();
        types.forEach((cd -> {
          metaTypes.putIfAbsent(cd.name(), DataType.String);
        }));
        logger.debug("Meta types: {}", metaTypes);
        DataRow metaRow = DataRow.create(metaTypes);
        types.forEach(cd -> {
          String typeName = cd.typeName();
          if (defn.isCompatibleTypeNames()) {
            if (cd.type() == DataType.Boolean) {
              typeName = "bool";
            } else if (cd.type() == DataType.Integer || cd.type() == DataType.Long) {
              typeName = "int";
            } else {
              typeName = typeName.toLowerCase(Locale.ROOT);
            }
          }
          metaRow.put(cd.name(), typeName);
        });
        String insertTitle = "";
        if (!Strings.isNullOrEmpty(title)) {
          insertTitle = "\"name\":\"" + title + "\",";
        }
        String insertDesc = "";
        if (!Strings.isNullOrEmpty(description)) {
          insertTitle = "\"description\":\"" + description.trim() + "\",";
        }
        start = "{\"" + defn.getMetadataName() + "\":{" + insertTitle + insertDesc + "\"fields\":" + toJson(metaRow).toString() + "},\"" + defn.getDataName() + "\":" + OPEN_ARRAY;
      }
      return outputStream.write(Buffer.buffer(start));
    }
  }
  
  private Future<Void> end() {
    if (Strings.isNullOrEmpty(defn.getDataName())) {
      return outputStream.write(CLOSE_ARRAY);
    } else {
      return outputStream.write(CLOSE_ARRAY_AND_OBJECT);
    }
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    Pipeline pipelineDefn = pipeline.getDefinition();
    if (pipelineDefn != null) {
      this.title = pipelineDefn.getTitle();
      this.description = pipelineDefn.getDescription();
    }
    return input.getStream().pipeTo(formattingStream);
  }
  
}
