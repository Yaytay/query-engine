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
package uk.co.spudsoft.query.exec.fmts.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Double;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.fmts.CustomDecimalFormatter;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.logging.Log;


/**
 * Output {@link uk.co.spudsoft.query.exec.FormatInstance} that generates JSON output.
 * <P>
 * The JSON structure itself is a single array, with one object for each row.
 * 
 * @author jtalbut
 */
public final class FormatJsonInstance implements FormatInstance {
  
  private static final Logger logger = LoggerFactory.getLogger(FormatJsonInstance.class);
  private static final JsonFactory JSON_FACTORY = new JsonFactory(); 
 
  private final WriteStream<Buffer> outputStream;
  private final FormatJson defn;
  private final FormattingWriteStream formattingStream;
  private final PipelineContext pipelineContext;
  private final Log log;
  
  private static final Buffer EMPTY_OBJECT = Buffer.buffer("{}");
  private static final Buffer OPEN_ARRAY = Buffer.buffer("[");
  private static final Buffer OPEN_ARRAY_NL = Buffer.buffer("[\n  ");
  private static final Buffer COMMA = Buffer.buffer(",");
  private static final Buffer CLOSE_ARRAY = Buffer.buffer("]");
  private static final Buffer CLOSE_ARRAY_AND_OBJECT = Buffer.buffer("]}");
  private static final Buffer NL_CLOSE_ARRAY = Buffer.buffer("\n]");
  private static final Buffer NL_CLOSE_ARRAY_AND_OBJECT = Buffer.buffer("\n]}");
  private static final Buffer NL_CLOSE_ARRAY_AND_OBJECT_INDENTED = Buffer.buffer("\n  ]\n}");
  private final AtomicBoolean started = new AtomicBoolean();
  private final int baseIndent;
  private Types types;
  private String title;
  private String description;
  
  private final ValueFormatters valueFormatters;
  
  /**
   * Constructor.
   * @param outputStream The WriteStream that the data is to be sent to.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.  The contained requestContext must have the rowCount updated at the end.
   * @param defn The definition of the output.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatJsonInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatJsonInstance(WriteStream<Buffer> outputStream, PipelineContext pipelineContext, FormatJson defn) {
    this.outputStream = outputStream;
    this.defn = defn;
    
    this.valueFormatters = defn.toValueFormatters("\"", "\"", true);
    
    this.baseIndent = Strings.isNullOrEmpty(defn.getDataName()) ? 1 : 2;
    
    this.pipelineContext = pipelineContext;
    this.log = new Log(logger, pipelineContext);

    this.formattingStream = new FormattingWriteStream(pipelineContext
            , outputStream
            , v -> {
              return Future.succeededFuture();
            }
            , row -> {
              try {                
                if (row.isEmpty()) {
                  return Future.succeededFuture();
                } else {
                  Buffer startBuffer;
                  boolean firstRow = false;
                  if (started.get()) {
                    startBuffer = COMMA;
                  } else {
                    started.set(true);
                    firstRow = true;
                    startBuffer = start();
                  }
                  Buffer rowBuffer = toJsonBuffer(row, firstRow);
                  Buffer outBuffer = Buffer.buffer(startBuffer.length() + rowBuffer.length());
                  outBuffer.appendBuffer(startBuffer);
                  outBuffer.appendBuffer(rowBuffer);
                  return outputStream.write(outBuffer);
                }
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            }
            , rows -> {
              pipelineContext.getRequestContext().setRowsWritten(rows);
              try {
                return end();
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            }
    );
  }
  
  Buffer toJsonBuffer(DataRow row, boolean firstRow) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try (JsonGenerator gen = JSON_FACTORY.createGenerator(out)) {
      if (defn.getPrettiness() > 1) {
        gen.setPrettyPrinter(new PrettyPrinterIndent(baseIndent));
      }
      if (defn.getPrettiness() == 1) {
        gen.writeRaw('\n');
      }
      gen.writeStartObject();
      
      row.forEach((cd, value) -> {
        try {
          if (value == null) {
            if (firstRow || defn.isOutputNullValues()) {
              gen.writeNullField(cd.name());
            }
          } else {
            switch (cd.type()) {
              case Boolean:
                gen.writeFieldName(cd.name());
                gen.writeRawValue(valueFormatters.getBooleanFormatter(cd.name()).format(pipelineContext, value));
                break ;
                
              case Double:
              case Float:
                  gen.writeFieldName(cd.name());
                  CustomDecimalFormatter formatter = valueFormatters.getDecimalFormatter(cd.name());
                  if (formatter.mustBeEncodedAsString()) {
                    gen.writeString(formatter.format(pipelineContext, value));
                  } else {
                    gen.writeRawValue(formatter.format(pipelineContext, value));
                  }
                break ;
                
              case Integer:
              case Long:
                if (value instanceof Number numValue) {
                  gen.writeNumberField(cd.name(), numValue.longValue());
                }
                break ;
                
              case Null:
                gen.writeNullField(cd.name());
                break ;
                
              case String:
                if (value instanceof String stringValue) {
                  gen.writeStringField(cd.name(), stringValue);
                } else {
                  gen.writeStringField(cd.name(), value.toString());
                }
                break ;
                
              case Date:
                gen.writeStringField(cd.name(), valueFormatters.getDateFormatter(cd.name()).format(pipelineContext, value));
                break ;
                
              case DateTime:
                Object formatted = valueFormatters.getDateTimeFormatter(cd.name()).format(pipelineContext, value);
                if (formatted instanceof String stringFormatted) {
                  gen.writeStringField(cd.name(), stringFormatted);
                } else if (formatted instanceof Long longFormatted) {
                  gen.writeNumberField(cd.name(), longFormatted);
                }
                break ;
                
              case Time:
                gen.writeStringField(cd.name(), valueFormatters.getTimeFormatter(cd.name()).format(pipelineContext, value));
                break ;
                
              default:
                log.warn().log("Field {} if of unknown type {} with value {} ({})", cd.name(), cd.type(), value, value.getClass());
                throw new IllegalStateException("Field of unknown type " + cd.type());
            }
          }
        } catch (Throwable ex) {
          log.warn().log("Failed to write JSON field {} with value {} ({})", cd.name(), value, value == null ? null : value.getClass(), ex);
        }
        
      });
      gen.writeEndObject();
    }
    
    return Buffer.buffer(out.toByteArray());
  }
  
  private Buffer start() throws IOException {
    if (Strings.isNullOrEmpty(defn.getDataName())) {
      if (defn.getPrettiness() > 1) {
        return OPEN_ARRAY_NL;
      } else {
        return OPEN_ARRAY;
      }
    } else {
      String start;
      if (Strings.isNullOrEmpty(defn.getMetadataName())) {
        if (defn.getPrettiness() > 1) {
          start = "{\n  \"" + defn.getDataName() + "\" : " + OPEN_ARRAY;
        } else {
          start = "{\"" + defn.getDataName() + "\":" + OPEN_ARRAY;
        }
      } else {
        log.debug().log("Data types: {}", types);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (JsonGenerator gen = JSON_FACTORY.createGenerator(out)) {
          if (defn.getPrettiness() > 1) {
            gen.setPrettyPrinter(new PrettyPrinterIndent(1));
          }
          
          gen.writeStartObject();
          if (!Strings.isNullOrEmpty(title)) {
            gen.writeStringField("name", title);
          }
          if (!Strings.isNullOrEmpty(description)) {
            gen.writeStringField("description", description.trim());            
          }
          gen.writeFieldName("fields");
          
          gen.writeStartObject();
          types.forEach(cd -> {
            String typeName = cd.typeName();
            if (defn.isCompatibleTypeNames()) {
              if (cd.type() == DataType.Boolean) {
                typeName = "bool";
              } else if (cd.type() == DataType.Integer || cd.type() == DataType.Long) {
                typeName = "int";
              } else {
                typeName = typeName.toLowerCase(Locale.ROOT);
              }
            }
            try {
              gen.writeStringField(cd.name(), typeName);
            } catch (Throwable ex) {
              log.warn().log("Failed to write JSON meta {} with value {}", cd.name(), typeName, ex);
            }
            
          });
          gen.writeEndObject();
          
          gen.writeEndObject();
        }

        if (defn.getPrettiness() > 1) {
          start = "{\n  \"" + defn.getMetadataName() + "\" : " + out.toString(StandardCharsets.UTF_8) + ",\n  \"" + defn.getDataName() + "\" : " + OPEN_ARRAY;
        } else {
          start = "{\"" + defn.getMetadataName() + "\":" + out.toString(StandardCharsets.UTF_8) + ",\"" + defn.getDataName() + "\":" + OPEN_ARRAY;
        }
      }
      return Buffer.buffer(start);
    }
  }

  private Future<Void> end() throws IOException {
    if (started.get()) {
      Buffer tail;
      if (defn.getPrettiness() > 1) {
        tail = Strings.isNullOrEmpty(defn.getDataName()) ? NL_CLOSE_ARRAY : NL_CLOSE_ARRAY_AND_OBJECT_INDENTED;
      } else if (defn.getPrettiness() == 1) {
        tail = Strings.isNullOrEmpty(defn.getDataName()) ? NL_CLOSE_ARRAY : NL_CLOSE_ARRAY_AND_OBJECT;
      } else {
        tail = Strings.isNullOrEmpty(defn.getDataName()) ? CLOSE_ARRAY : CLOSE_ARRAY_AND_OBJECT;
      }
      return outputStream.end(tail);
    } else {
      if (defn.isCompatibleEmpty()) {
        return outputStream.end(EMPTY_OBJECT);
      } else {
        Buffer startBuffer = start();
        Buffer tailBuffer = Strings.isNullOrEmpty(defn.getDataName()) ? CLOSE_ARRAY : CLOSE_ARRAY_AND_OBJECT;
        Buffer outBuffer = Buffer.buffer(startBuffer.length() + tailBuffer.length());
        outBuffer.appendBuffer(startBuffer);
        outBuffer.appendBuffer(tailBuffer);
        return outputStream.end(outBuffer);
      } 
    }
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.types = input.getTypes();
    Pipeline pipelineDefn = pipeline.getDefinition();
    if (pipelineDefn != null) {
      this.title = pipelineDefn.getTitle();
      this.description = pipelineDefn.getDescription();
    }
    return input.getStream().pipeTo(formattingStream);
  }
  
}
