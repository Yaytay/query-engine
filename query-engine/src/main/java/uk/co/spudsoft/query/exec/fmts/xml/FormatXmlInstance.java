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
import javax.xml.stream.XMLStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Null;
import static uk.co.spudsoft.query.defn.DataType.Time;

import static uk.co.spudsoft.query.defn.FormatXml.NAME_CHAR_REGEX;
import static uk.co.spudsoft.query.defn.FormatXml.NAME_START_REGEX;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.main.Coalesce;

/**
 * Handles the formatting of data into XML format as part of a data processing pipeline.
 * This class implements the FormatInstance interface to define the behaviour required for formatting data output.
 * It uses XML writing utilities to generate structured XML documents based on the provided definition and data rows.
 */
public final class FormatXmlInstance implements FormatInstance {

  private static final Logger logger = LoggerFactory.getLogger(FormatXmlInstance.class);
  @SuppressWarnings("ConstantName")
  private static final XMLOutputFactory xmlOutputFactory = WstxOutputFactory.newFactory();

  private final FormatXml defn;
  private final OutputWriteStreamWrapper streamWrapper;
  private final FormattingWriteStream formattingStream;

  private final ValueFormatters valueFormatters;

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
    this.defn = definition;
    outputStream.setWriteQueueMaxSize(1000);
    this.streamWrapper = new OutputWriteStreamWrapper(outputStream);
    this.valueFormatters = defn.toValueFormatters("", "", false);
    this.formattingStream = createFormattingWriteStream(
      outputStream,
      streamWrapper,
      started,
      this::start,
      this::outputRow,
      this::close
    );
  }

  /**
   * Functional interface for the start operation.
   */
  @FunctionalInterface
  interface StartFunction {
    void start() throws Exception;
  }

  /**
   * Functional interface for processing a row.
   */
  @FunctionalInterface
  interface ProcessRowFunction {
    void processRow(DataRow row) throws Throwable;
  }

  /**
   * Functional interface for the close operation.
   */
  @FunctionalInterface
  interface CloseFunction {
    void close() throws Exception;
  }

  /**
   * Creates a FormattingWriteStream with the standard XML formatting pattern.
   * This factory method can be reused by other XML-based format instances.
   *
   * @param outputStream The WriteStream that the data is to be sent to.
   * @param streamWrapper The OutputWriteStreamWrapper for the output stream.
   * @param started AtomicBoolean to track if formatting has started.
   * @param startFunction Function to call when starting the output.
   * @param processRowFunction Function to call for each DataRow.
   * @param closeFunction Function to call when closing the output.
   * @return A configured FormattingWriteStream.
   */
  static FormattingWriteStream createFormattingWriteStream(
    WriteStream<Buffer> outputStream,
    OutputWriteStreamWrapper streamWrapper,
    AtomicBoolean started,
    StartFunction startFunction,
    ProcessRowFunction processRowFunction,
    CloseFunction closeFunction) {

    return new FormattingWriteStream(streamWrapper
      , v -> Future.succeededFuture()
      , row -> {
      logger.trace("Got row {}", row);
      if (!started.get()) {
        try {
          startFunction.start();
        } catch (Throwable ex) {
          logger.warn("Failed to start XML writer: ", ex);
          return Future.failedFuture(ex);
        }
      }
      if (!row.isEmpty()) {
        try {
          processRowFunction.processRow(row);
        } catch (Throwable ex) {
          logger.warn("Failed to output row: ", ex);
          return Future.failedFuture(ex);
        }
      }
      return Future.succeededFuture();
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
          startFunction.start();
        } catch (Throwable ex) {
          logger.warn("Failed to start XML writer: ", ex);
          return Future.failedFuture(ex);
        }
      }
      try {
        closeFunction.close();
        return Future.succeededFuture();
      } catch (Throwable ex) {
        return Future.failedFuture(ex);
      }
    }
    );
  }

  static String formatValue(ValueFormatters valueFormatters, String elementName, DataType type, Comparable<?> value) {
    if (value == null) {
      return null;
    } else {
      switch (type) {
        case Boolean:
          return valueFormatters.getBooleanFormatter(elementName).format(value);

        case Double:
        case Float:
          return valueFormatters.getDecimalFormatter(elementName).format(value);

        case Integer:
        case Long:
          if (value instanceof Number numValue) {
            return Long.toString(numValue.longValue());
          } else {
            return value.toString();
          }

        case Null:
          return null;

        case String:
          if (value instanceof String stringValue) {            
            return stringValue;
          } else {
            return value.toString();
          }

        case Date:
          return valueFormatters.getDateFormatter(elementName).format(value);

        case DateTime:
          Object formatted = valueFormatters.getDateTimeFormatter(elementName).format(value);
          if (formatted instanceof String stringFormatted) {
            return stringFormatted;
          } else if (formatted != null) {
            return formatted.toString();
          } else {
            return null;
          }

        case Time:
          return valueFormatters.getTimeFormatter(elementName).format(value);

        default:
          logger.warn("Field {} if of unknown type {} with value {} ({})", elementName, type, value, value.getClass());
          throw new IllegalStateException("Field of unknown type " + type);
      }
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

  private void start() throws Exception {
    started.set(true);

    String encoding = Coalesce.coalesce(defn.getEncoding(), "utf-8");
    writer = xmlOutputFactory.createXMLStreamWriter(streamWrapper, Charset.forName(encoding).name());
    if (defn.isXmlDeclaration()) {
      writer.writeStartDocument(encoding, "1.0");
      if (defn.isIndent()) {
        writer.writeCharacters("\n");
      }
    }
    writer.writeStartElement(getName(defn.getDocName(), "data"));
    logger.trace("Document started");
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
    return getName(nameMap, defn.getFieldInitialLetterFix(), defn.getFieldInvalidLetterFix(), original, defaultValue);
  }

  static String getName(Map<String, String> nameMap, String fieldInitialLetterFix, String fieldInvalidLetterFix, String original, String defaultValue) {
    if (original == null) {
      return defaultValue;
    }
    if (fieldInitialLetterFix == null) {
      fieldInitialLetterFix = "";
    }
    if (fieldInvalidLetterFix == null) {
      fieldInvalidLetterFix = "";
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

  private void outputRow(DataRow row) throws Throwable {
    if (defn.isIndent()) {
      writer.writeCharacters("\n  ");
    }
    writer.writeStartElement(getName(defn.getRowName(), "row"));
    int fieldNumber = 0;
    for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext();) {
      ++fieldNumber;
      ColumnDefn columnDefn = iter.next();
      Comparable<?> v = row.get(columnDefn.key());
      if (v != null) {
        if (defn.isFieldsAsAttributes()) {
          writer.writeAttribute(
                  getName(columnDefn.name(), "field" +  fieldNumber)
                  , formatValue(valueFormatters, columnDefn.name(), columnDefn.type(), v)
          );
        } else {
          if (defn.isIndent()) {
            writer.writeCharacters("\n    ");
          }
          if (columnDefn.type() == DataType.String && !defn.getCharacterReferences().isEmpty()) {
            // If there are character references to replace we need to handle strings differently
            // We only do character reference replacements in strings.
            String stringValue = formatValue(valueFormatters, columnDefn.name(), columnDefn.type(), v);
            writer.writeStartElement(getName(columnDefn.name(), "field" + fieldNumber));
            writeCharactersWithReplacementCharacterRefs(stringValue);
            writer.writeEndElement();
          } else {
            writer.writeStartElement(getName(columnDefn.name(), "field" + fieldNumber));
            writer.writeCharacters(formatValue(valueFormatters, columnDefn.name(), columnDefn.type(), v));
            writer.writeEndElement();
          }
        }
      }
    }
    if (defn.isIndent() && !defn.isFieldsAsAttributes()) {
      writer.writeCharacters("\n  ");
    }
    writer.writeEndElement();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    return input.getStream().pipeTo(formattingStream);
  }
    
  private void writeCharactersWithReplacementCharacterRefs(String text) throws XMLStreamException {
    StringBuilder normalChars = new StringBuilder();

    for (int i = 0; i < text.length();) {
      boolean found = false;

      for (Map.Entry<String, String> entry : defn.getCharacterReferenceMap().entrySet()) {
        String pattern = entry.getKey();
        if (text.startsWith(pattern, i)) {
          // Write accumulated normal characters first
          if (normalChars.length() > 0) {
            writer.writeCharacters(normalChars.toString());
            normalChars.setLength(0);
          }
          // Write entity reference
          writer.writeEntityRef(entry.getValue());
          i += pattern.length();
          found = true;
          break;
        }
      }

      if (!found) {
        normalChars.append(text.charAt(i));
        i++;
      }
    }

    // Write any remaining normal characters
    if (normalChars.length() > 0) {
      writer.writeCharacters(normalChars.toString());
    }
  }
}
