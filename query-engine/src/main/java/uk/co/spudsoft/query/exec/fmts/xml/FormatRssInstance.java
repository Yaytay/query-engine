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
package uk.co.spudsoft.query.exec.fmts.xml;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.FormatRss;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.fmts.xlsx.OutputWriteStreamWrapper;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.logging.Log;

/**
 * Handles the formatting of data into Rss format as part of a data processing pipeline. This class implements the FormatInstance
 * interface to define the behaviour required for formatting data output. It uses XML writing utilities to generate structured XML
 * documents based on the provided definition and data rows.
 */
public final class FormatRssInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatRssInstance.class);
  @SuppressWarnings("ConstantName")
  private static final XMLOutputFactory xmlOutputFactory = WstxOutputFactory.newFactory();

  private static final String DEFAULT_CUSTOM_NAMESPACE = "https://yaytay.github.io/query-engine/rss";

  private static final Set<String> STD_ITEM_ELEMENTS = ImmutableSet.<String>builder()
    .add("title")
    .add("link")
    .add("description")
    .add("author")
    .add("category")
    .add("comments")
    .add("enclosure")
    .add("guid")
    .add("pubDate")
    .add("source")
    .build();

  private final FormatRss defn;
  private final PipelineContext pipelineContext;
  private final Log log;
  
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;

  private final ValueFormatters valueFormatters;

  private final String customNamespace;

  private final AtomicBoolean started = new AtomicBoolean();
  private XMLStreamWriter writer;

  private AtomicLong rowNum = new AtomicLong(0);
  private boolean closed = false;

  private final Map<String, String> nameMap = new HashMap<>();

  private Types types;

  /**
   * Constructor.
   *
   * @param definition The formatting definition for the output.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.  The contained requestContext must have the rowCount updated at the end.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  public FormatRssInstance(FormatRss definition, PipelineContext pipelineContext, WriteStream<Buffer> outputStream) {
    this.defn = definition;
    this.pipelineContext = pipelineContext;
    this.log = new Log(logger, pipelineContext);
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.valueFormatters = defn.toValueFormatters("", "", false);
    this.customNamespace = Strings.isNullOrEmpty(definition.getCustomNamespace()) ? DEFAULT_CUSTOM_NAMESPACE : definition.getCustomNamespace();
    this.formattingStream = FormatXmlInstance.createFormattingWriteStream(
            pipelineContext,
            log,
            outputStream,
            streamWrapper,
            started,
            this::start,
            this::outputRow,
            this::close
          );
  }

  private void start() throws Exception {
    started.set(true);

    writer = xmlOutputFactory.createXMLStreamWriter(streamWrapper, StandardCharsets.UTF_8.name());
    writer.writeStartElement("rss");
    writer.writeNamespace("custom", customNamespace);
    writer.writeAttribute("version", "2.0");
    writer.writeCharacters("\n  ");
    writer.writeStartElement("channel");
  }

  private void close() throws Exception {
    if (!closed) {
      closed = true;
      // channel
      writer.writeEndElement();
      // rss
      writer.writeEndElement();
      writer.writeEndDocument();
      writer.close();
      streamWrapper.close();
    }
  }

  /**
   * Returns a future that completes when the associated {@code WriteStream} has reached its final state,
   * indicating that the stream has either been closed or finished writing.
   *
   * @return a {@code Future<Void>} that completes when the final operation on the stream is complete.
   */
  public Future<Void> getFinalFuture() {
    return streamWrapper.getFinalFuture();
  }

  private void outputRow(DataRow row) throws Throwable {
    writer.writeCharacters("\n    ");
    writer.writeStartElement("item");

    int fieldNumber = 0;
    for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext();) {
      ++fieldNumber;
      ColumnDefn columnDefn = iter.next();
      Comparable<?> v = row.get(columnDefn.name());

      String fieldName = columnDefn.name();

      writer.writeCharacters("\n      ");
      if (STD_ITEM_ELEMENTS.contains(fieldName)) {
        writer.writeStartElement(fieldName);
      } else {
        writer.writeStartElement(customNamespace, fieldName);
      }
      if (v != null) {
        writer.writeCharacters(FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, columnDefn.name(), columnDefn.type(), v));
      }
      writer.writeEndElement();
    }
    writer.writeCharacters("\n    ");
    writer.writeEndElement();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }

}
