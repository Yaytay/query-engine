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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.FormatAtom;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the formatting of data into Atom format as part of a data processing pipeline. This class implements the FormatInstance
 * interface to define the behaviour required for formatting data output. It uses XML writing utilities to generate structured XML
 * documents based on the provided definition and data rows.
 */
public final class FormatAtomInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatAtomInstance.class);
  @SuppressWarnings("ConstantName")
  private static final XMLOutputFactory xmlOutputFactory = WstxOutputFactory.newFactory();

  private static final String METADATA_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata";
  private static final String DATASERVICES_NAMESPACE = "http://schemas.microsoft.com/ado/2007/08/dataservices";

  private final FormatAtom defn;
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;
  private final String requestUrl;
  private final String baseUrl;

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
   * @param requestUrl The base URL (or path) that was used for this request.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  public FormatAtomInstance(FormatAtom definition, String requestUrl, WriteStream<Buffer> outputStream) {
    this.defn = definition.withDefaults();
    this.requestUrl = requestUrl;
    if (requestUrl.endsWith("/")) {
      this.baseUrl = requestUrl;
    } else {
      this.baseUrl = requestUrl + "/";
    }
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
      writer.writeStartDocument("utf-8", "1.0");
      writer.writeStartElement("feed");
      writer.writeDefaultNamespace("http://www.w3.org/2005/Atom");
      writer.writeNamespace("m", METADATA_NAMESPACE);
      writer.writeNamespace("d", DATASERVICES_NAMESPACE);

      writeElement("id", requestUrl);
      writeElement("title", defn.getName());
      writeElement("updated", OffsetDateTime.now(ZoneId.of("UTC")));
    } catch (XMLStreamException ex) {
      logger.warn("Failed to create XMLStreamWriter: ", ex);
      throw new IOException(ex);
    }
  }

  private void close() throws Exception {
    if (!closed) {
      closed = true;
      writer.writeEndElement();
      writer.writeEndDocument();
      writer.close();
      streamWrapper.close();
    }
  }

  private String getName(String original, String defaultValue) {
    return FormatXmlInstance.getName(nameMap, this.defn.getFieldInitialLetterFix(), this.defn.getFieldInvalidLetterFix(), original, defaultValue);
  }

  void writeElement(String elementName, Comparable<?> content) throws XMLStreamException {
    if (content == null) {
      writer.writeStartElement(getName(elementName, "field"));
      writer.writeEndElement();
    } else {
      writer.writeStartElement(getName(elementName, "field"));
      writer.writeCharacters(formatValue(content));
      writer.writeEndElement();
    }
  }

  static String formatValue(Comparable<?> value) {
    if (value == null) {
      return null;
    } else if (value instanceof Timestamp) {
      return formatValue((Timestamp) value);
    } else if (value instanceof LocalDateTime) {
      return formatValue((LocalDateTime) value);
    } else if (value instanceof LocalTime) {
      return formatValue((LocalTime) value);
    } else if (value instanceof Time) {
      return formatValue(((Time) value).toLocalTime());
    } else if (value instanceof java.sql.Date) {
      return formatValue(((java.sql.Date) value).toLocalDate());
    } else if (value instanceof java.util.Date) {
      return formatValue(LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneOffset.UTC));
    } else if (value instanceof Boolean) {
      return ((Boolean) value) ? "true" : "false";
    } else {
      return value.toString();
    }
  }

  static String formatValue(Timestamp ts) {
    return formatValue(ts.toLocalDateTime());
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

  private String getType(Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Integer) {
      return "Edm.Int32";
    } else if (value instanceof Long) {
      return "Edm.Int64";
    } else if (value instanceof Double) {
      return "Edm.Double";
    } else if (value instanceof String) {
      return null;
    } else if (value instanceof LocalDateTime) {
      return "Edm.DateTime";
    } else if (value instanceof Timestamp) {
      return "Edm.DateTime";
    } else if (value instanceof BigDecimal) {
      return "Edm.Decimal";
    } else if (value instanceof Date) {
      return "Edm.DateTime";
    } else if (value instanceof Boolean) {
      return "Edm.Boolean";
    } else {
      logger.debug("Atom Data type: {}", value.getClass());
      return null;
    }
  }

  private void outputRow(DataRow row) throws Throwable {
    try {
      writer.writeStartElement("entry");

      writeElement("id", baseUrl + rowNum.incrementAndGet());
      writeElement("title", defn.getName());
      writeElement("updated", OffsetDateTime.now(ZoneId.of("UTC")));

      writer.writeStartElement("content");
      writer.writeAttribute("type", "application/xml");

      writer.writeStartElement("m", "properties", METADATA_NAMESPACE);

      row.forEach((k, v) -> {
        try {
          writer.writeStartElement("d", getName(k.name(), "field"), DATASERVICES_NAMESPACE);
          String type = getType(v);
          if (type != null) {
            writer.writeAttribute("m", METADATA_NAMESPACE, "type", type);
          }
          if (v == null) {
            writer.writeAttribute("m", METADATA_NAMESPACE, "null", "true");
          } else {
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
      writer.writeEndElement();

      writer.writeEndElement();
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
