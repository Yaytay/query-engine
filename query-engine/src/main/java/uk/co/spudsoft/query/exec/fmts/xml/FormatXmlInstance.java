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
import uk.co.spudsoft.query.defn.FormatXml;
import uk.co.spudsoft.query.exec.ColumnDefn;
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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static uk.co.spudsoft.query.defn.FormatXml.NAME_CHAR_REGEX;
import static uk.co.spudsoft.query.defn.FormatXml.NAME_START_REGEX;

/**
 * Handles the formatting of data into XML format as part of a data processing pipeline.
 * This class implements the FormatInstance interface to define the behaviour required for formatting data output.
 * It uses XML writing utilities to generate structured XML documents based on the provided definition and data rows.
 */
public class FormatXmlInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatXmlInstance.class);
  @SuppressWarnings("ConstantName")
  private static final XMLOutputFactory xmlOutputFactory = WstxOutputFactory.newFactory();

  private final FormatXml defn;
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;

  private final AtomicBoolean started = new AtomicBoolean();
  private XMLStreamWriter writer;

  private boolean closed = false;

  private final Map<String, String> nameMap = new HashMap<>();

  private Types types;

  /**
   * Constructor.
   * @param definition The formatting definition for the output.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  public FormatXmlInstance(FormatXml definition, WriteStream<Buffer> outputStream) {
    this.defn = definition.withDefaults();
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.formattingStream = new FormattingWriteStream(outputStream
      , v -> Future.succeededFuture()
      , row -> {
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
      writer = xmlOutputFactory.createXMLStreamWriter(streamWrapper, Charset.forName(defn.getEncoding()).name());
      if (defn.isXmlDeclaration()) {
        writer.writeStartDocument(defn.getEncoding(), "1.0");
        if (defn.isIndent()) {
          writer.writeCharacters("\n");
        }
      }
      writer.writeStartElement(getName(defn.getDocName(), "data"));
      logger.debug("Document started");
    } catch (XMLStreamException ex) {
      logger.warn("Failed to create XMLStreamWriter: ", ex);
      throw new IOException(ex);
    }
  }

  private void close() throws Exception {
    if (!closed) {
      closed = true;
      if (defn.isIndent()) {
        writer.writeCharacters("\n");
      }
      writer.writeEndElement();
      writer.writeEndDocument();
      writer.close();
      streamWrapper.close();
    }
  }

  static boolean appendIfValid(StringBuilder builder, int[] codePoints, int index, Pattern regex, String alternative, boolean lastWasValid) {
    String c = new String(codePoints, index, 1);
    if (!regex.matcher(c).matches()) {
      if (lastWasValid) {
        builder.append(alternative);
      }
      return false;
    } else {
      builder.append(c);
      return true;
    }
  }

  private String getName(String original, String defaultValue) {
    return getName(nameMap, defn.getFieldInitialLetterFix(), defn.getFieldInitialLetterFix(), original, defaultValue);
  }

  static String getName(Map<String, String> nameMap, String fieldInitialLetterFix, String fieldInvalidLetterFix, String original, String defaultValue) {
    if (original == null) {
      return defaultValue;
    }
    String result = nameMap.get(original);
    if (result != null) {
      return result;
    }
    if (NAME_START_REGEX.matcher(original).matches()) {
      nameMap.put(original, original);
      return original;
    }
    StringBuilder builder = new StringBuilder();
    int[] codePoints = original.codePoints().toArray();
    boolean lastWasValid = appendIfValid(builder, codePoints, 0, NAME_START_REGEX, fieldInitialLetterFix, true);
    for (int i = 1; i < codePoints.length; ++i) {
      lastWasValid = appendIfValid(builder, codePoints, i, NAME_CHAR_REGEX, fieldInvalidLetterFix, lastWasValid);
    }
    result = builder.toString();
    if (!NAME_START_REGEX.matcher(result).matches()) {
      result = defaultValue;
    }
    int offset = 1;
    String built = result;
    while (nameMap.containsKey(result)) {
      result = built + offset;
      ++offset;
    }

    nameMap.put(original, result);
    return result;
  }

  static String formatValue(Comparable<?> value) {
    if (value == null) {
      return null;
    } else {
      return value.toString();
    }
  }

  private void outputRow(DataRow row) throws Throwable {
      try {
        if (defn.isIndent()) {
          writer.writeCharacters("\n  ");
        }
        writer.writeStartElement(getName(defn.getRowName(), "row"));
        int fieldNumber = 0;
        for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext();) {
          ++fieldNumber;
          ColumnDefn columnDefn = iter.next();
          Comparable<?> v = row.get(columnDefn.name());
          if (v != null) {
            if (defn.isFieldsAsAttributes()) {
              writer.writeAttribute(getName(columnDefn.name(), "field" +  fieldNumber), formatValue(v));
            } else {
              if (defn.isIndent()) {
                writer.writeCharacters("\n    ");
              }
              writer.writeStartElement(getName(columnDefn.name(), "field" + fieldNumber));
              writer.writeCharacters(formatValue(v));
              writer.writeEndElement();
            }
          }
        }
        if (defn.isIndent()) {
          writer.writeCharacters("\n  ");
        }
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
