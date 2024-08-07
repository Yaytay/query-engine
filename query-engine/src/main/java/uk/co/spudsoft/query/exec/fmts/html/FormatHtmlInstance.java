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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.web.RequestContextHandler;


/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates HTML output.
 * <P>
 * The HTML structure is quite inflexible - it is a single table with a column for each defined field and a row for each row.
 * CSS classes are defined liberally to enable host systems to format the results nicely.
 * The following classes are used:
 * <UL>
 * <LI>{@code qetable} for the entire table.
 * <LI>{@code header} for the table header tr and for each th within.
 * <LI>{@code evenCol} for each even numbered column (in both header and body).
 * <LI>{@code oddCol} for each odd numbered column (in both header and body).
 * <LI>{@code evenRow} for each even numbered row in the body.
 * <LI>{@code oddRow} for each odd numbered row in the body.
 * </UL>
 * @author jtalbut
 */
public class FormatHtmlInstance implements FormatInstance {
 
  private final WriteStream<Buffer> outputStream;
  private final FormattingWriteStream formattingStream;
  
  private static final Buffer OPEN = Buffer.buffer("<table class=\"qetable\"><thead>\n");
  private static final Buffer ENDHEAD = Buffer.buffer("</thead><tbody>\n");
  private static final Buffer CLOSE = Buffer.buffer("</tbody></table>");
  private final AtomicBoolean started = new AtomicBoolean();
  
  private Types types;
  
  private int rowNum = 0;
  
  /**
   * Constructor.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatHtmlInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatHtmlInstance(WriteStream<Buffer> outputStream) {
    this.outputStream = outputStream;
    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> outputStream.write(OPEN)
            , row -> {
              if (!started.get()) {
                started.set(true);
                return outputStream.write(Buffer.buffer(headerFromRow()))
                        .compose(v -> outputStream.write(ENDHEAD))
                        .compose(v -> outputStream.write(Buffer.buffer(rowFromRow(row))))
                        ;
              } else {
                return outputStream.write(Buffer.buffer(rowFromRow(row)));
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
                return outputStream.write(Buffer.buffer(headerFromRow()))
                        .compose(v -> outputStream.write(ENDHEAD))
                        .compose(v -> outputStream.write(CLOSE))
                        .compose(v2 -> outputStream.end())
                        ;
              } else {
                return outputStream.write(CLOSE)
                        .compose(v2 -> outputStream.end())
                        ;
              }
            }
    );
  }
  
  private String headerFromRow() {
    StringBuilder header = new StringBuilder();
    header.append("<tr class=\"header\">");
    int colNum[] = {0};
    types.forEach((cd) -> {
      String evenCol = ((colNum[0]++ % 2 == 0) ? "evenCol" : "oddCol");
      header.append("<th class=\"header ").append(evenCol).append("\" >").append(cd.name()).append("</th>");
    });
    header.append("</tr>\n");
    return header.toString();
  }

  private String rowFromRow(DataRow row) {
    if (row.isEmpty()) {
      return "";
    }
    StringBuilder tr = new StringBuilder();
    String evenRow = ((rowNum++ % 2 == 0) ? "evenRow" : "oddRow");
    tr.append("<tr class=\"dataRow ").append(evenRow).append("\" >");
    int colNum = 0;
    for (Entry<String, Object> field : row.getMap().entrySet()) {
      String evenCol = ((colNum++ % 2 == 0) ? "evenCol" : "oddCol");
      Object value = field.getValue();
      tr.append("<td class=\"").append(evenRow).append(" ").append(evenCol).append("\">");
      if (value != null) {
        tr.append(value);
      }
      tr.append("</td>");
    }
    tr.append("</tr>\n");
    return tr.toString();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }
  
}
