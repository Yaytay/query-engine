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
package uk.co.spudsoft.query.exec.fmts.text;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.web.RequestContextHandler;

/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates text output.
 * <P>
 * There are a number of configuration properties passed in via a {@link uk.co.spudsoft.query.defn.FormatDelimited} object.
 * <P>
 * It is not possible to change the configuration for a {@link uk.co.spudsoft.query.exec.FormatInstance} each time a pipeline is run (unless the report design is a template)
 * so the common approach is to configure multiple different named delimited output formats for the user to choose from.
 * 
 * @author jtalbut
 */
public final class FormatDelimitedInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatDelimitedInstance.class);
  
  private final FormatDelimited defn;
  private final WriteStream<Buffer> outputStream;
  private final FormattingWriteStream formattingStream;
  private final AtomicBoolean started = new AtomicBoolean();

  private final ValueFormatters valueFormatters;
  
  private final Promise<Void> finalPromise;
  
  private Types types;

  /**
   * Constructor.
   * @param defn The definition of the format to be output
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatJsonInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatDelimitedInstance(FormatDelimited defn, WriteStream<Buffer> outputStream) {
    this.defn = defn;
    this.outputStream = outputStream;
    this.finalPromise = Promise.<Void>promise();
    
    this.valueFormatters = defn.toValueFormatters(defn.getOpenQuote(), defn.getCloseQuote(), false);
    
    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> Future.succeededFuture()
            , row -> {
              if (started.compareAndSet(false, true)) {
                return outputHeader().compose(v -> outputRow(row));
              } else {
                return outputRow(row);
              }
            }
            , rowCount -> {
              if (!started.get()) {
                outputHeader();
              }
              Context vertxContext = Vertx.currentContext();
              if (vertxContext != null) {
                RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                if (requestContext != null) {
                  requestContext.setRowsWritten(rowCount);
                }
              }              
              return outputStream.end()
                      .andThen(ar -> {
                        finalPromise.handle(ar);
                      });
            }
    );
  }

  /**
   * Returns a future that completes when the associated {@code WriteStream} has reached its final state,
   * indicating that the stream has either been closed or finished writing.
   *
   * @return a {@code Future<Void>} that completes when the final operation on the stream is complete.
   */
  public Future<Void> getFinalFuture() {
    return finalPromise.future();
  }
  
  private Future<Void> outputHeader() {
    if (defn.hasHeaderRow()) {
      StringBuilder headerRow = new StringBuilder();
      
      if (defn.hasBom()) {
        headerRow.append("\uFEFF");
      }
      
      AtomicBoolean first = new AtomicBoolean(true);
      types.forEach(cd-> {
        if (!first.compareAndSet(true, false)) {
          headerRow.append(defn.getDelimiter());
        }
        outputEncodedQuotedString(headerRow, cd.name());
      });
      headerRow.append(defn.getNewline());
      return outputStream.write(Buffer.buffer(headerRow.toString()));
    } else if (defn.hasBom()) { 
      return outputStream.write(Buffer.buffer("\uFEFF"));
    } else {
      return Future.succeededFuture();
    }
  }
  
  private void outputEncodedQuotedString(StringBuilder row, String value) {
    row.append(defn.getOpenQuote());
    row.append(encodeCloseQuote(defn, value));
    row.append(defn.getCloseQuote());
  }

  private Future<Void> outputRow(DataRow row) {
    logger.trace("Outputting row: {}", row);
    if (row.isEmpty()) {
      return Future.succeededFuture();
    }
    String outputRow = generateOutputRow(row);
    return outputStream.write(Buffer.buffer(outputRow.getBytes(StandardCharsets.UTF_8)));
  }

  String generateOutputRow(DataRow row) {
    StringBuilder outputRow = new StringBuilder();
    AtomicBoolean first = new AtomicBoolean(true);
    row.forEach((cd, v) -> {
      if (!first.compareAndSet(true, false)) {
        outputRow.append(defn.getDelimiter());
      }
      try {
        if (v != null) {
          String stringValue;
          switch (cd.type()) {
            case Boolean:
              outputRow.append(valueFormatters.getBooleanFormatter(cd.name()).format(v));              
              break;
            case Double:
            case Float:
              outputRow.append(valueFormatters.getDecimalFormatter(cd.name()).format(v));
              break;
            case Integer:
            case Long:
              outputRow.append(v);
              break;
            case Date:
              stringValue = valueFormatters.getDateFormatter(cd.name()).format(v);
              if (defn.isQuoteTemporal()) {
                outputEncodedQuotedString(outputRow, stringValue);
              } else {
                outputRow.append(stringValue);
              }
              break;
            case DateTime:
              Object objectValue = valueFormatters.getDateTimeFormatter(cd.name()).format(v);
              if (objectValue instanceof String s) {
                stringValue = s;
              } else {
                stringValue = objectValue == null ? null : objectValue.toString();
              }
              if (defn.isQuoteTemporal()) {
                outputEncodedQuotedString(outputRow, stringValue);
              } else {
                outputRow.append(stringValue);
              }
              break;              
            case Time:
              stringValue = valueFormatters.getTimeFormatter(cd.name()).format(v);
              if (defn.isQuoteTemporal()) {
                outputEncodedQuotedString(outputRow, stringValue);
              } else {
                outputRow.append(stringValue);
              }
              break;
            case String:
            default:
              outputEncodedQuotedString(outputRow, v.toString());
              break;
          }
        }
      } catch (Throwable ex) {
        logger.warn("Failed to output field {} with value {}: ", cd.name(), v, ex);
      }
    });
    outputRow.append(defn.getNewline());
    return outputRow.toString();
  }
  
  static String encodeCloseQuote(FormatDelimited defn, String string) {
    if (!Strings.isNullOrEmpty(defn.getCloseQuote())) {
      String replacement = null;
      if (!Strings.isNullOrEmpty(defn.getEscapeCloseQuote())) {
        replacement = defn.getEscapeCloseQuote() + defn.getCloseQuote();
      } else if (!Strings.isNullOrEmpty(defn.getReplaceCloseQuote())) {
        replacement = defn.getReplaceCloseQuote();
      }
      if (replacement != null) {
        replacement = Matcher.quoteReplacement(replacement);
        string = string.replaceAll(Pattern.quote(defn.getCloseQuote()), replacement);
      }
    }
    return string;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }
  
}
