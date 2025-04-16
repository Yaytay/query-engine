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
import com.google.common.collect.ImmutableSet;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
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
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.fmts.xlsx.OutputWriteStreamWrapper;
import uk.co.spudsoft.query.web.RequestContextHandler;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the formatting of data into Rss format as part of a data processing pipeline. This class implements the FormatInstance
 * interface to define the behaviour required for formatting data output. It uses XML writing utilities to generate structured XML
 * documents based on the provided definition and data rows.
 */
public final class FormatRssInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatRssInstance.class);
  @SuppressWarnings("ConstantName")
  private static final XMLOutputFactory xmlOutputFactory = WstxOutputFactory.newFactory();

  private static final String CUSTOM_NAMESPACE = "https://yaytay.github.io/query-engine/rss";

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
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;

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
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  public FormatRssInstance(FormatRss definition, WriteStream<Buffer> outputStream) {
    this.defn = definition.withDefaults();
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.formattingStream = new FormattingWriteStream(outputStream,
             v -> Future.succeededFuture(),
             row -> {
              logger.info("Got row {}", row);
              if (!started.get()) {
                try {
                  start();
                } catch (Throwable ex) {
                  return Future.failedFuture(ex);
                }
              }
              if (!row.isEmpty()) {
                try {
                  outputRow(row);
                } catch (Throwable ex) {
                  return Future.failedFuture(ex);
                }
              }
              if (streamWrapper.writeQueueFull()) {
                Promise<Void> promise = Promise.promise();
                streamWrapper.drainHandler(v -> promise.complete());
                return promise.future();
              } else {
                return Future.succeededFuture();
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
                try {
                  start();
                } catch (Throwable ex) {
                  return Future.failedFuture(ex);
                }
              }
              try {
                close();
                return Future.succeededFuture();
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            }
    );
  }

  private void start() throws IOException {
    started.set(true);

    try {
      writer = xmlOutputFactory.createXMLStreamWriter(streamWrapper, StandardCharsets.UTF_8.name());
      writer.writeStartElement("rss");
      writer.writeNamespace("custom", CUSTOM_NAMESPACE);
      writer.writeAttribute("version", "2.0");
      writer.writeCharacters("\n  ");
      writer.writeStartElement("channel");
    } catch (XMLStreamException ex) {
      logger.warn("Failed to create XMLStreamWriter: ", ex);
      throw new IOException(ex);
    }
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

  private String getName(String original, String defaultValue) {
    return FormatXmlInstance.getName(nameMap, this.defn.getFieldInitialLetterFix(), this.defn.getFieldInvalidLetterFix(), original, defaultValue);
  }

  static String formatValue(Comparable<?> value) {
    if (value == null) {
      return null;
    } else if (value instanceof LocalDateTime) {
      return formatValue((LocalDateTime) value);
    } else if (value instanceof LocalDate) {
      return formatValue((LocalDate) value);
    } else if (value instanceof LocalTime) {
      return formatValue((LocalTime) value);
    } else if (value instanceof Boolean) {
      return ((Boolean) value) ? "true" : "false";
    } else {
      return value.toString();
    }
  }

  static String formatValue(LocalTime lt) {
    return lt.toString();
  }

  static String formatValue(LocalDateTime ldt) {
    return ldt.toString();
  }

  static String formatValue(LocalDate ld) {
    return ld.toString();
  }

  private void outputRow(DataRow row) throws Throwable {
    try {
      writer.writeCharacters("\n    ");
      writer.writeStartElement("item");

      row.forEach((k, v) -> {
        try {
          String fieldName = getName(k.name(), "field");
          writer.writeCharacters("\n      ");
          if (STD_ITEM_ELEMENTS.contains(fieldName)) {
            writer.writeStartElement(fieldName);
          } else {
            writer.writeStartElement("custom", fieldName, CUSTOM_NAMESPACE);
          }
          if (v != null) {
            writer.writeCharacters(formatValue(v));
          }
          writer.writeEndElement();
        } catch (XMLStreamException ex) {
          if (v == null)  {
            logger.warn("Failed to output null value: ", ex);
          } else {
            logger.warn("Failed to output {} value \"{}\": ", v, v.getClass(), ex);

          }
          throw new RuntimeException(ex);
        }
      });
      writer.writeCharacters("\n    ");
      writer.writeEndElement();

    } catch (Throwable ex) {
      logger.warn("Failed to output row: ", ex);
      throw ex;
    }
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }

}
