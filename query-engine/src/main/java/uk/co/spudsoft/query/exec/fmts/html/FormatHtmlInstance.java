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


/**
 *
 * @author jtalbut
 */
public class FormatHtmlInstance implements FormatInstance {
 
  private final WriteStream<Buffer> outputStream;
  private final FormattingWriteStream formattingStream;
  
  private static final Buffer OPEN = Buffer.buffer("<table class=\"qetable\"><thead>\n");
  private static final Buffer ENDHEAD = Buffer.buffer("</thead><tbody>\n");
  private static final Buffer CLOSE = Buffer.buffer("</tbody></table>");
  private final AtomicBoolean started = new AtomicBoolean();
  
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
                return outputStream.write(Buffer.buffer(headerFromRow(row)))
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
                RequestContext requestContext = Vertx.currentContext().getLocal("req");
                if (requestContext != null) {
                  requestContext.setRowsWritten(rows);
                }
              }
              if (!started.get()) {
                started.set(true);
                return outputStream.write(Buffer.buffer(headerFromRow(DataRow.EMPTY_ROW)))
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
  
  private String headerFromRow(DataRow row) {
    StringBuilder header = new StringBuilder();
    header.append("<tr class=\"header\">");
    int colNum = 0;
    for (String field : row.getMap().keySet()) {
      String evenCol = ((colNum++ % 2 == 0) ? "evenCol" : "oddCol");
      header.append("<th class=\"header ").append(evenCol).append("\" >").append(field).append("</th>");
    }
    header.append("</tr>\n");
    return header.toString();
  }

  private String rowFromRow(DataRow row) {
    StringBuilder header = new StringBuilder();
    String evenRow = ((rowNum++ % 2 == 0) ? "evenRow" : "oddRow");
    header.append("<tr class=\"dataRow ").append(evenRow).append("\" >");
    int colNum = 0;
    for (Entry<String, Object> field : row.getMap().entrySet()) {
      String evenCol = ((colNum++ % 2 == 0) ? "evenCol" : "oddCol");
      header.append("<td class=\"").append(evenRow).append(" ").append(evenCol).append("\">").append(field.getValue()).append("</td>");
    }
    header.append("</tr>\n");
    return header.toString();
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
