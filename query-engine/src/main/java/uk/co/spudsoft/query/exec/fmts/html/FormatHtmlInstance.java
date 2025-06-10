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
package uk.co.spudsoft.query.exec.fmts.html;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Null;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.FormatHtml;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.CustomBooleanFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDecimalFormatter;
import uk.co.spudsoft.query.web.RequestContextHandler;

/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates HTML output.
 * <P>
 * The HTML structure is quite inflexible - it is a single table with a column for each defined field and a row for each row. CSS
 * classes are defined liberally to enable host systems to format the results nicely. The following classes are used:
 * <UL>
 * <LI>{@code qetable} for the entire table.
 * <LI>{@code header} for the table header tr and for each th within.
 * <LI>{@code evenCol} for each even numbered column (in both header and body).
 * <LI>{@code oddCol} for each odd numbered column (in both header and body).
 * <LI>{@code evenRow} for each even numbered row in the body.
 * <LI>{@code oddRow} for each odd numbered row in the body.
 * </UL>
 *
 * @author jtalbut
 */
public class FormatHtmlInstance implements FormatInstance {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FormatHtmlInstance.class);

  private final WriteStream<Buffer> outputStream;
  private final FormattingWriteStream formattingStream;

  private static final Buffer OPEN = Buffer.buffer("<table class=\"qetable\"><thead>\n");
  private static final String ENDHEAD = "</thead><tbody>\n";
  private static final String CLOSE = "</tbody></table>";
  private final AtomicBoolean started = new AtomicBoolean();

  private final DateTimeFormatter dateFormatter;
  private final CustomDateTimeFormatter dateTimeFormatter;
  private final DateTimeFormatter timeFormatter;

  private final CustomDecimalFormatter decimalFormatter;
  private final CustomBooleanFormatter booleanFormatter;
  
  private final Promise<Void> finalPromise;

  private Types types;

  private int rowNum = 0;

  /**
   * Constructor.
   *
   * @param defn The definition of the format to be output
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatHtmlInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatHtmlInstance(FormatHtml defn, WriteStream<Buffer> outputStream) {
    this.outputStream = outputStream;
    this.finalPromise = Promise.<Void>promise();

    if (Strings.isNullOrEmpty(defn.getDateFormat())) {
      this.dateFormatter = null;
    } else {
      this.dateFormatter = DateTimeFormatter.ofPattern(defn.getDateFormat());
    }

    dateTimeFormatter = new CustomDateTimeFormatter(defn.getDateTimeFormat());

    if (Strings.isNullOrEmpty(defn.getTimeFormat())) {
      this.timeFormatter = null;
    } else {
      this.timeFormatter = DateTimeFormatter.ofPattern(defn.getTimeFormat());
    }

    this.decimalFormatter = new CustomDecimalFormatter(defn.getDecimalFormat());
    this.booleanFormatter = new CustomBooleanFormatter(defn.getBooleanFormat(), "", "", false);

    this.formattingStream = new FormattingWriteStream(outputStream,
            v -> outputStream.write(OPEN),
            row -> {
              if (!started.get()) {
                started.set(true);
                
                String headerAndFirstRow = headerFromRow()
                        .append(ENDHEAD)
                        .append(rowFromRow(row))
                        .toString();
                Buffer buffer = Buffer.buffer(headerAndFirstRow);
                return outputStream.write(buffer);
              } else {
                return outputStream.write(Buffer.buffer(rowFromRow(row)));
              }
            },
            rows -> {
              Context vertxContext = Vertx.currentContext();
              if (vertxContext != null) {
                RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                if (requestContext != null) {
                  requestContext.setRowsWritten(rows);
                }
              }
              if (!started.get()) {
                started.set(true);
                String headerAndClose = headerFromRow()
                        .append(ENDHEAD)
                        .append(CLOSE)
                        .toString();
                Buffer buffer = Buffer.buffer(headerAndClose);
                return outputStream.write(buffer)
                        .compose(v2 -> outputStream.end());
              } else {
                return outputStream.write(Buffer.buffer(CLOSE))
                        .compose(v2 -> outputStream.end())
                        .andThen(ar -> {
                          finalPromise.handle(ar);
                        });
              }
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
  
  private StringBuilder headerFromRow() {
    StringBuilder header = new StringBuilder();
    header.append("<tr class=\"header\">");
    int colNum[] = {0};
    types.forEach((cd) -> {
      String evenCol = ((++colNum[0] % 2 == 0) ? "evenCol" : "oddCol");
      header.append("<th class=\"header ").append(evenCol).append("\" >").append(cd.name()).append("</th>");
    });
    header.append("</tr>\n");
    return header;
  }

  private String rowFromRow(DataRow row) {
    if (row.isEmpty()) {
      return "";
    }
    StringBuilder tr = new StringBuilder();
    String evenRow = ((++rowNum % 2 == 0) ? "evenRow" : "oddRow");
    tr.append("<tr class=\"dataRow ").append(evenRow).append("\" >");

    boolean isEvenField[] = {false};

    row.forEach((cd, value) -> {
      String evenCol = (isEvenField[0] ? "evenCol" : "oddCol");
      isEvenField[0] = !isEvenField[0];
      tr.append("<td class=\"").append(evenRow).append(" ").append(evenCol).append("\">");
      if (value != null) {
        switch (cd.type()) {
          case Boolean:
            tr.append(booleanFormatter.format(value));
            break;

          case Double:
          case Float:
            if (value instanceof Number numberValue) {
              tr.append(decimalFormatter.format(numberValue));
            }
            break;

          case Integer:
          case Long:
            if (value instanceof Number numValue) {
              tr.append(numValue.longValue());
            }
            break;

          case Null:
            break;

          case String:
            if (value instanceof String stringValue) {
              tr.append(stringValue);
            } else {
              tr.append(value.toString());
            }
            break;

          case Date:
            if (dateFormatter == null) {
              tr.append(value.toString());
            } else {
              if (value instanceof TemporalAccessor ta) {
                tr.append(dateFormatter.format(ta));
              }
            }
            break;

          case DateTime:
            if (value instanceof LocalDateTime ldt) {
              Object formatted = dateTimeFormatter.format(ldt);
              tr.append(formatted);
            } else {
              logger.warn("DateTime value is not LocalDateTime (it's {} of {})", value.getClass(), value);
              tr.append(value.toString());
            }
            break;

          case Time:
            if (timeFormatter == null) {
              tr.append(value.toString());
            } else {
              if (value instanceof TemporalAccessor ta) {
                tr.append(timeFormatter.format(ta));
              }
            }
            break;

          default:
            logger.warn("Field {} if of unknown type {} with value {} ({})", cd.name(), cd.type(), value, value.getClass());
            throw new IllegalStateException("Field of unknown type " + cd.type());
        }
      }
      tr.append("</td>");
    });
    tr.append("</tr>\n");
    return tr.toString();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }

}
