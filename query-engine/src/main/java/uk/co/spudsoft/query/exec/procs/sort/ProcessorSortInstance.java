/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.procs.sort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.streams.ReadStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Null;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class ProcessorSortInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorSortInstance.class);
  
  private static String TEMP_DIR = System.getProperty("java.io.tmpdir");
  private static int MEMORY_LIMIT = 1 << 22; // 4MB
  
  private final Vertx vertx;
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorSort definition;
  
  private SortingStream<DataRow> stream;
  
  private Types types;
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorSortInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorSort definition) {
    this.vertx = vertx;
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;    
  }  

  @Override
  public String getId() {
    return definition.getId();
  }

  public static void setTempDir(String TEMP_DIR) {
    ProcessorSortInstance.TEMP_DIR = TEMP_DIR;
  }

  public static void setMemoryLimit(int MEMORY_LIMIT) {
    ProcessorSortInstance.MEMORY_LIMIT = MEMORY_LIMIT;
  }

  /**
   * Purely for test purposes.
   * @return The configured limit for this processor instance.
   */
  public List<String> getSort() {
    return definition.getFields();
  }

  @Override
  public ReadStream<DataRow> getReadStream() {
    return stream;
  }

  /**
   * This serializer, and its associated deserializer, are only aimed at serving the needs of the SortingStream and are not suitable for general purpose serialization.
   * Specifically, they require the Types to be known by the deserializer in advance.
  */
  private byte[] dataRowSerializer(DataRow row) throws IOException {
    int sizeGuess = row.bytesSize() + 4 * row.size();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(sizeGuess)) {
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext(); ) {
          ColumnDefn cd = iter.next();
          Object value = row.get(cd.name());
          if (value == null) {
            oos.writeByte(~cd.type().ordinal());
          } else {
            oos.writeByte(cd.type().ordinal());
            switch(cd.type()) {
              case Boolean:
                oos.writeBoolean((Boolean) value);
                break;
              case Date:
                oos.writeObject((LocalDate) value);
                break;
              case DateTime:
                oos.writeObject((LocalDateTime) value);
                break;
              case Double:
                oos.writeDouble((Double) value);
                break;
              case Float:
                oos.writeFloat((Float) value);
                break;
              case Integer:
                oos.writeInt((Integer) value);
                break;
              case Long:
                oos.writeLong((Long) value);
                break;
              case Null:
                break;
              case String:
                String stringValue = (String) value;
                byte[] bytes = stringValue.getBytes(StandardCharsets.UTF_8);
                int length = bytes.length;
                oos.writeInt(length);
                oos.write(bytes);
                break;
              case Time:
                oos.writeObject((LocalTime) value);
                break;
            }
          }
        }
      }
      byte[] result = baos.toByteArray();
      if (result.length > sizeGuess) {
        logger.warn("Guessed at {} bytes, but was actually {} bytes", sizeGuess, result.length);
      }
      return result;
    }
  }
  
  /**
   * This serializer, and its associated deserializer, are only aimed at serving the needs of the SortingStream and are not suitable for general purpose serialization.
   * Specifically, they require the Types to be known by the deserializer in advance.
   * @param bytes
   * @return 
   */
  private DataRow dataRowDeserializer(byte[] bytes) throws IOException  {
    DataRow result = DataRow.create(types);
    try (ByteArrayInputStream baos = new ByteArrayInputStream(bytes)) {
      try (ObjectInputStream ois = new ObjectInputStream(baos)) {
        for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext(); ) {
          ColumnDefn cd = iter.next();
          if (ois.available() < 1) {
            return result;
          }
          byte typeOrd = ois.readByte();
          if (typeOrd <= 0) {
            result.put(cd.name(), cd.type(), null);
          } else {
            DataType type = DataType.fromOrdinal(typeOrd);
            switch(type) {
              case DataType.Boolean:
                assert(cd.type() == DataType.Boolean);
                result.put(cd.name(), cd.type(), ois.readBoolean());
                break;
              case DataType.Date:
                assert(cd.type() == DataType.Date);
                result.put(cd.name(), cd.type(), (LocalDate) ois.readObject());
                break;
              case DataType.DateTime:
                assert(cd.type() == DataType.DateTime);
                result.put(cd.name(), cd.type(), (LocalDateTime) ois.readObject());
                break;
              case DataType.Double:
                assert(cd.type() == DataType.Double);
                result.put(cd.name(), cd.type(), ois.readDouble());
                break;
              case DataType.Float:
                assert(cd.type() == DataType.Float);
                result.put(cd.name(), cd.type(), ois.readFloat());
                break;
              case DataType.Integer:
                assert(cd.type() == DataType.Integer);
                result.put(cd.name(), cd.type(), ois.readInt());
                break;
              case DataType.Long:
                assert(cd.type() == DataType.Long);
                result.put(cd.name(), cd.type(), ois.readLong());
                break;
              case DataType.Null:
                break;
              case DataType.String:
                assert(cd.type() == DataType.String);
                int length = ois.readInt();
                byte[] stringBytes = new byte[length];
                ois.read(stringBytes);
                result.put(cd.name(), cd.type(), new String(stringBytes, StandardCharsets.UTF_8));
                break;
              case DataType.Time:
                assert(cd.type() == DataType.Time);
                result.put(cd.name(), cd.type(), (LocalTime) ois.readObject());
                break;
            }
          }
        }
      }
    } catch (ClassNotFoundException ex) {
      logger.error("ObjectInputStream threw ClassNotFoundException, which shouldn't happen: ", ex);
      throw new IOException("Unable to deserialize stream", ex);
    }
    return result;
  }
  
  private String sanitiseSourceName(String name) {
    return name.replaceAll("[\\p{Cntrl}`!\\\"|$%^&*(){}\\[\\];:'@#~,./\\\\<>]*", "_");
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStream<DataRow> input) {
    
    FileSystem fileSystem = vertx.fileSystem();
    String tempDir = TEMP_DIR;
    
    return fileSystem.mkdirs(tempDir)
            .onSuccess(v -> {
              this.stream = new SortingStream<>(context
                    , fileSystem
                    , () -> DataRowComparator.createChain(types, definition.getFields())
                    , this::dataRowSerializer
                    , this::dataRowDeserializer
                    , tempDir
                    , "ProcessSort_" + sanitiseSourceName(sourceNameTracker.toString()) + "_"
                    , MEMORY_LIMIT
                    , DataRow::bytesSize
              );
            });
    
  }
  
}
