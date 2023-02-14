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
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.defn.FormatXlsxColumn;
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

/**
 *
 * @author jtalbut
 */
public class FormatXlsxInstance implements FormatInstance {
 
  private final FormatXlsx definition;
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;
  
  private final AtomicBoolean started = new AtomicBoolean();
  private XlsxWriter writer;
  
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
              if (!started.get()) {
                started.set(true);
                TableDefinition tableDefintion = tableDefinitionFromRow(row);
                writer = new XlsxWriter(tableDefintion);
                try {
                  writer.startFile(streamWrapper);
                } catch (IOException ex) {
                  return Future.failedFuture(ex);
                }
              }
              try {
                writer.outputRow(values(row));
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
                RequestContext requestContext = Vertx.currentContext().getLocal("req");
                if (requestContext != null) {
                  requestContext.setRowsWritten(rows);
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
    if (Strings.isNullOrEmpty(one)) {
      return two;
    }
    return one;
  }
  
  private String getUsernameFromContext() {
    Context vertxContext = Vertx.currentContext();
    if (vertxContext != null) {
      RequestContext requestContext = vertxContext.getLocal("req");
      if (requestContext != null) {
        return requestContext.getUsername();
      }      
    }
    return "Unknown";
  }
  
  private FontDefinition getDefaultFont() {
    return new FontDefinition("Calibri", 11);
  }
  
  private ColourDefinition getDefaultColours() {
    return new ColourDefinition("FFFFFF", "000000");
  }
  
  private TableDefinition tableDefinitionFromRow(DataRow row) {
    List<ColumnDefinition> columns = new ArrayList<>(row.size());
    FormatXlsxColumn nonFormat = FormatXlsxColumn.builder().build();
    row.forEach((k, v) -> {
      ColumnDefinition defn = null;
      if (definition.getColumns() != null) {
        FormatXlsxColumn formatColumn = definition.getColumns().get(k);
        if (formatColumn != null) {
          defn = formatColumn.toColumnDefinition(k, row.getType(k));
        }
      } 
      if (defn == null) {
        defn = nonFormat.toColumnDefinition(k, row.getType(k));
      }
      columns.add(defn);
    });
    
    TableDefinition defn = new TableDefinition(
            "SpudSoft Query Engine"
            , coalesce(definition.getSheetName(), "Data")
            , coalesce(Strings.isNullOrEmpty(definition.getCreator()) ? getUsernameFromContext() : definition.getCreator(), "Data")
            , definition.isGridLines()
            , definition.isHeaders()
            , definition.getHeaderFont() == null ? getDefaultFont() : definition.getHeaderFont().toFontDefinition()
            , definition.getBodyFont() == null ? getDefaultFont() : definition.getBodyFont().toFontDefinition()
            , definition.getHeaderColours() == null ? getDefaultColours() : definition.getHeaderColours().toColourDefinition()
            , definition.getEvenColours() == null ? getDefaultColours() : definition.getEvenColours().toColourDefinition()
            , definition.getOddColours() == null ? getDefaultColours() : definition.getOddColours().toColourDefinition()
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
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }

  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The caller WILL modify the state of the returned WriteStream.")
  public WriteStream<DataRow> getWriteStream() {
    return formattingStream;
  }
  
}