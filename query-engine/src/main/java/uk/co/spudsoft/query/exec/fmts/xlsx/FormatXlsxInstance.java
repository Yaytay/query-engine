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
package uk.co.spudsoft.query.exec.fmts.xlsx;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Long;
import static uk.co.spudsoft.query.defn.DataType.String;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.defn.FormatXlsxColours;
import uk.co.spudsoft.query.defn.FormatXlsxColumn;
import uk.co.spudsoft.query.defn.FormatXlsxFont;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.xlsx.ColourDefinition;
import uk.co.spudsoft.xlsx.ColumnDefinition;
import uk.co.spudsoft.xlsx.FontDefinition;
import uk.co.spudsoft.xlsx.TableDefinition;
import uk.co.spudsoft.xlsx.XlsxWriter;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.web.RequestContextHandler;

/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates XLSX output.
 * <P>
 * There are a number of configuration properties passed in via a {@link uk.co.spudsoft.query.defn.FormatXlsx} object.
 * <P>
 * Typically an organisation will decide upon a consistent style for all XLSX output and just configure a single
 * XLSX output format in each pipeline definition.
 * 
 * @author jtalbut
 */
public class FormatXlsxInstance implements FormatInstance {
 
  private static final Logger logger = LoggerFactory.getLogger(FormatXlsxInstance.class);
  
  private final FormatXlsx definition;
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;
  
  private final AtomicBoolean started = new AtomicBoolean();
  private XlsxWriter writer;
  
  private Types types;
  
  /**
   * Constructor.
   * @param definition The formatting definition for the output.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatXlsxInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatXlsxInstance(FormatXlsx definition, WriteStream<Buffer> outputStream) {
    this.definition = definition;
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> Future.succeededFuture()
            , row -> {
              logger.info("Got row {}", row);
              if (!started.get()) {
                started.set(true);
                TableDefinition tableDefintion = tableDefinition();
                writer = new XlsxWriter(tableDefintion);
                try {
                  writer.startFile(streamWrapper);
                } catch (IOException ex) {
                  return Future.failedFuture(ex);
                }
              }
              try {
                if (!row.isEmpty()) {
                  writer.outputRow(values(row));
                }
              } catch (IOException ex) {
                return Future.failedFuture(ex);
              }
              if (streamWrapper.writeQueueFull()) {
                Promise<Void> promise = Promise.promise();
                streamWrapper.drainHandler(v -> promise.complete());
                return promise.future();
              } else {
                return Future.succeededFuture();
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
              if (!started.get()) {
                started.set(true);
                TableDefinition tableDefintion = tableDefinition();
                writer = new XlsxWriter(tableDefintion);
                try {
                  writer.startFile(streamWrapper);
                } catch (IOException ex) {
                  return Future.failedFuture(ex);
                }
              }
              try {
                writer.close();
                streamWrapper.close();
                return Future.succeededFuture();
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            }
    );
  }
  
  private String coalesce(String one, String two) {
    if (!Strings.isNullOrEmpty(one)) {
      return one;
    }
    return two;
  }
  
  private String coalesce(String one, String two, String three) {
    if (!Strings.isNullOrEmpty(one)) {
      return one;
    }
    if (!Strings.isNullOrEmpty(two)) {
      return two;
    }
    return three;
  }
  
  private String getUsernameFromContext() {
    Context vertxContext = Vertx.currentContext();
    if (vertxContext != null) {
      RequestContext requestContext = RequestContextHandler.getRequestContext(vertxContext);
      if (requestContext != null) {
        return requestContext.getUsername();
      }      
    }
    return "Unknown";
  }
  
  static FontDefinition getFontDefinition(FormatXlsxFont defn) {
    if (defn != null) {
      return defn.toFontDefinition();
    } else {
      return new FontDefinition("Calibri", 11);
    }
  }
  
  static ColourDefinition getColourDefinition(FormatXlsxColours defn) {
    if (defn != null) {
      return defn.toColourDefinition();
    } else {
      return new ColourDefinition("000000", "FFFFFF");
    }
  }
  
  private String defaultFormatFor(DataType type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case Boolean -> null;
      case Date -> definition.getDefaultDateFormat();
      case DateTime -> definition.getDefaultDateTimeFormat();
      case Double -> null;
      case Float -> null;
      case Integer -> null;
      case Long -> null;
      case String -> null;
      case Time -> definition.getDefaultTimeFormat();
      default -> null;
    };
  }
    
  private TableDefinition tableDefinition() {
    List<ColumnDefinition> columns = new ArrayList<>(types.size());
    FormatXlsxColumn nonFormat = FormatXlsxColumn.builder().build();
    types.forEach((cd) -> {
      ColumnDefinition defn = null;
      FormatXlsxColumn formatColumn = definition.getColumnsMap().get(cd.name());
      if (formatColumn != null) {
        defn = formatColumn.toColumnDefinition(cd.name(), cd.type(), this::defaultFormatFor);
      } else {
        defn = nonFormat.toColumnDefinition(cd.name(), cd.type(), this::defaultFormatFor);
      }
      columns.add(defn);
    });
    
    TableDefinition defn = new TableDefinition(
            "SpudSoft Query Engine"
            , coalesce(definition.getSheetName(), "Data")
            , coalesce(definition.getCreator(), getUsernameFromContext(), "Data")
            , definition.isGridLines()
            , definition.isHeaders()
            , getFontDefinition(definition.getHeaderFont())
            , getFontDefinition(definition.getBodyFont())
            , getColourDefinition(definition.getHeaderColours())
            , getColourDefinition(definition.getEvenColours())
            , getColourDefinition(definition.getOddColours())
            , columns
    );
    
    return defn;
  }
  
  private static List<Object> values(DataRow row) {
    List<Object> values = new ArrayList<>(row.size());
    row.forEach((k, v) -> values.add(v));
    return values;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }
}
