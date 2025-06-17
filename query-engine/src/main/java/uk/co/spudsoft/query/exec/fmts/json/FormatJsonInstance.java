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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import static io.vertx.sqlclient.impl.Utils.toJson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Double;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.CustomDecimalFormatter;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.web.RequestContextHandler;


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
  
  private static final Buffer OPEN_ARRAY = Buffer.buffer("[");
  private static final Buffer COMMA = Buffer.buffer(",");
  private static final Buffer CLOSE_ARRAY = Buffer.buffer("]");
  private static final Buffer CLOSE_ARRAY_AND_OBJECT = Buffer.buffer("]}");
  private final AtomicBoolean started = new AtomicBoolean();
  private Types types;
  private String title;
  private String description;
  
  private final ValueFormatters valueFormatters;
  
  /**
   * Constructor.
   * @param outputStream The WriteStream that the data is to be sent to.
   * @param defn The definition of the output.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatJsonInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatJsonInstance(WriteStream<Buffer> outputStream, FormatJson defn) {
    this.outputStream = outputStream;
    this.defn = defn;
    
    this.valueFormatters = defn.toValueFormatters("\"", "\"", true);

    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> {
              return start();
            }
            , row -> {
              try {
                if (row.isEmpty()) {
                  return Future.succeededFuture();
                } else if (started.get()) {
                  Buffer buffer = COMMA.copy().appendBuffer(toJsonBuffer(row));
                  return outputStream.write(buffer);
                } else {
                  started.set(true);
                  return outputStream.write(toJsonBuffer(row));
                }
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
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
              return end()
                      .compose(v2 -> outputStream.end())
                      ;
            }
    );
  }
  
  Buffer toJsonBuffer(DataRow row) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    try (JsonGenerator gen = JSON_FACTORY.createGenerator(out)) {
      gen.writeStartObject();
      
      row.forEach((cd, value) -> {
        try {
          if (value == null) {
            gen.writeNullField(cd.name());
          } else {
            switch (cd.type()) {
              case Boolean:
                gen.writeFieldName(cd.name());
                gen.writeRawValue(valueFormatters.getBooleanFormatter(cd.name()).format(value));
                break ;
                
              case Double:
              case Float:
                  gen.writeFieldName(cd.name());
                  CustomDecimalFormatter formatter = valueFormatters.getDecimalFormatter(cd.name());
                  if (formatter.mustBeEncodedAsString()) {
                    gen.writeString(formatter.format(value));
                  } else {
                    gen.writeRawValue(formatter.format(value));
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
                gen.writeStringField(cd.name(), valueFormatters.getDateFormatter(cd.name()).format(value));
                break ;
                
              case DateTime:
                Object formatted = valueFormatters.getDateTimeFormatter(cd.name()).format(value);
                if (formatted instanceof String stringFormatted) {
                  gen.writeStringField(cd.name(), stringFormatted);
                } else if (formatted instanceof Long longFormatted) {
                  gen.writeNumberField(cd.name(), longFormatted);
                }
                break ;
                
              case Time:
                gen.writeStringField(cd.name(), valueFormatters.getTimeFormatter(cd.name()).format(value));
                break ;
                
              default:
                logger.warn("Field {} if of unknown type {} with value {} ({})", cd.name(), cd.type(), value, value.getClass());
                throw new IllegalStateException("Field of unknown type " + cd.type());
            }
          }
        } catch (Throwable ex) {
          logger.warn("Failed to write JSON field {} with value {} ({})", cd.name(), value, value == null ? null : value.getClass());
        }
        
      });
      
      gen.writeEndObject();
    }
    
    return Buffer.buffer(out.toByteArray());
  }
  
  private Future<Void> start() {
    if (Strings.isNullOrEmpty(defn.getDataName())) {
      return outputStream.write(OPEN_ARRAY);
    } else {
      String start;
      if (Strings.isNullOrEmpty(defn.getMetadataName())) {
        start = "{\"" + defn.getDataName() + "\":" + OPEN_ARRAY;
      } else {
        logger.debug("Data types: {}", types);
        Types metaTypes = new Types();
        types.forEach((cd -> {
          metaTypes.putIfAbsent(cd.name(), DataType.String);
        }));
        logger.debug("Meta types: {}", metaTypes);
        DataRow metaRow = DataRow.create(metaTypes);
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
          metaRow.put(cd.name(), typeName);
        });
        String insertTitle = "";
        if (!Strings.isNullOrEmpty(title)) {
          insertTitle = "\"name\":\"" + title + "\",";
        }
        String insertDesc = "";
        if (!Strings.isNullOrEmpty(description)) {
          insertDesc = "\"description\":\"" + description.trim() + "\",";
        }
        start = "{\"" + defn.getMetadataName() + "\":{" + insertTitle + insertDesc + "\"fields\":" + toJson(metaRow).toString() + "},\"" + defn.getDataName() + "\":" + OPEN_ARRAY;
      }
      return outputStream.write(Buffer.buffer(start));
    }
  }
  
  private Future<Void> end() {
    if (Strings.isNullOrEmpty(defn.getDataName())) {
      return outputStream.write(CLOSE_ARRAY);
    } else {
      return outputStream.write(CLOSE_ARRAY_AND_OBJECT);
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
