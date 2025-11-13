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
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatAtom;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.fmts.xlsx.OutputWriteStreamWrapper;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Null;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.logging.Log;

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
  private final PipelineContext pipelineContext;
  private final Log log;
  
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;
  private final String requestUrl;
  private final String baseUrl;

  private final ValueFormatters valueFormatters;

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
  public FormatAtomInstance(FormatAtom definition, PipelineContext pipelineContext, WriteStream<Buffer> outputStream) {
    this.defn = definition;
    this.pipelineContext = pipelineContext;
    this.log = new Log(logger, pipelineContext);
    this.requestUrl = pipelineContext.getRequestContext().getUrl();
    this.baseUrl = endsWith(requestUrl, "/");
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.valueFormatters = defn.toValueFormatters("", "", false);
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

  /**
   * Returns a future that completes when the associated {@code WriteStream} has reached its final state,
   * indicating that the stream has either been closed or finished writing.
   *
   * @return a {@code Future<Void>} that completes when the final operation on the stream is complete.
   */
  public Future<Void> getFinalFuture() {
    return streamWrapper.getFinalFuture();
  }

  static String endsWith(String s, String suffix) {
    if (s == null) {
      return null;
    } else if (s.endsWith(suffix)) {
      return s;
    } else {
      return s + suffix;
    }
  }

  private void start() throws Exception {
    started.set(true);

    writer = xmlOutputFactory.createXMLStreamWriter(streamWrapper, StandardCharsets.UTF_8.name());
    writer.writeStartDocument("utf-8", "1.0");
    writer.writeStartElement("feed");
    writer.writeDefaultNamespace("http://www.w3.org/2005/Atom");
    writer.writeNamespace("m", METADATA_NAMESPACE);
    writer.writeNamespace("d", DATASERVICES_NAMESPACE);

    writeElement("id", DataType.String, requestUrl);
    writeElement("title", DataType.String, defn.getName());
    writeElement("updated", DataType.DateTime, OffsetDateTime.now(ZoneId.of("UTC")));
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

  void writeElement(String elementName, DataType type, Comparable<?> value) throws XMLStreamException {
    writer.writeStartElement(getName(elementName, "field"));

    String formatted = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, elementName, type, value);
    if (formatted != null) {
      writer.writeCharacters(formatted);
    }
    writer.writeEndElement();
  }

  static String getType(DataType type) {
    return switch (type) {
      case Null -> "Null";
      case Integer -> "Edm.Int32";
      case Long -> "Edm.Int64";
      case Float -> "Edm.Single";
      case Double -> "Edm.Double";
      case String -> "Edm.String";
      case Boolean -> "Edm.Boolean";
      case Date -> "Edm.Date";
      case DateTime -> "Edm.DateTime";
      case Time -> "Edm.Time";
    };
  }

  private void outputRow(DataRow row) throws Throwable {
    writer.writeStartElement("entry");

    writeElement("id", DataType.String, baseUrl + rowNum.incrementAndGet());
    writeElement("title", DataType.String, defn.getName());
    writeElement("updated", DataType.DateTime, OffsetDateTime.now(ZoneId.of("UTC")));

    writer.writeStartElement("content");
    writer.writeAttribute("type", "application/xml");

    writer.writeStartElement("m", "properties", METADATA_NAMESPACE);

    row.forEach((k, v) -> {
      try {
        writer.writeStartElement("d", getName(k.name(), "field"), DATASERVICES_NAMESPACE);
        String type = getType(k.type());
        if (type != null) {
          writer.writeAttribute("m", METADATA_NAMESPACE, "type", type);
        }
        if (v == null) {
          writer.writeAttribute("m", METADATA_NAMESPACE, "null", "true");
        } else {
          writer.writeCharacters(FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, k.name(), k.type(), v));
        }
        writer.writeEndElement();
      } catch (XMLStreamException ex) {
        if (v == null)  {
          log.warn().log("Failed to output null value: ", ex);
        } else {
          log.warn().log("Failed to output {} value \"{}\": ", v, v.getClass(), ex);

        }
        throw new RuntimeException(ex);
      }
    });
    writer.writeEndElement();

    writer.writeEndElement();
    writer.writeEndElement();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }

}
