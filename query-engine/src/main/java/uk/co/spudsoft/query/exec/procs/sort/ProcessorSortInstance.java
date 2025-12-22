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
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import static uk.co.spudsoft.query.defn.DataType.Boolean;
import static uk.co.spudsoft.query.defn.DataType.Date;
import static uk.co.spudsoft.query.defn.DataType.DateTime;
import static uk.co.spudsoft.query.defn.DataType.Double;
import static uk.co.spudsoft.query.defn.DataType.Float;
import static uk.co.spudsoft.query.defn.DataType.Integer;
import static uk.co.spudsoft.query.defn.DataType.Time;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.AbstractProcessor;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to sort the stream of {@link uk.co.spudsoft.query.exec.DataRow} objects.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.ProcessorSort} that has a list of field names to sort by (which can be preceded by "-" to sort descending).
 * <P>
 * The sort processor cannot pass on any rows until it has received all rows - it is the only part of the Query Engine that absolutely cannot stream data.
 * <P>
 * There tempDir and memoryLimit configuration properties that are not settable in the pipeline definition - these must be configured globally as part of the overall configuration of the Query Engine (see {@link uk.co.spudsoft.query.main.Parameters}).
 *
 * @author jtalbut
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class ProcessorSortInstance extends AbstractProcessor {

  @SuppressWarnings("constantname")
  private static final Logger slf4jlogger = LoggerFactory.getLogger(ProcessorSortInstance.class);

  private static String tempDir = System.getProperty("java.io.tmpdir");
  private static int memoryLimit = 1 << 22; // 4MB

  private final ProcessorSort definition;

  private SortingStream<DataRow> stream;

  private Types types;

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link uk.co.spudsoft.query.defn.SourcePipeline} is being run.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The requestContext should not be modified by this class")
  public ProcessorSortInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorSort definition, String name) {
    super(slf4jlogger, vertx, meterRegistry, auditor, pipelineContext, name);
    this.definition = definition;
  }

  /**
   * The temporary directory used for files created when a stream exceeds the memory limit.
   * @param tempDir the temporary directory used for files created when a stream exceeds the memory limit.
   */
  public static void setTempDir(String tempDir) {
    ProcessorSortInstance.tempDir = tempDir;
  }

  /**
   * Set the global memory limit.
   * <p>
   * There is only one memory limit value in the entire process, but each ProcessorSortInstance is permitted to use that amount of memory.
   * i.e. the limit is not shared amongst all instances, even though the configuration is.
   * @param memoryLimit the global memory limit.
   */
  public static void setMemoryLimit(int memoryLimit) {
    ProcessorSortInstance.memoryLimit = memoryLimit;
  }

  /**
   * This serializer, and its associated deserializer, are only aimed at serving the needs of the SortingStream and are not suitable for general purpose serialization.
   * Specifically, they require the Types to be known by the deserializer in advance.
  */
  byte[] dataRowSerializer(DataRow row) throws IOException {
    int sizeGuess = row.bytesSize() + 4 * row.size();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(sizeGuess)) {
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext();) {
          ColumnDefn cd = iter.next();
          Object value = row.get(cd.name());
          if (value == null) {
            oos.writeByte(~cd.type().ordinal());
          } else {
            oos.writeByte(cd.type().ordinal());
            switch (cd.type()) {
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
              default:
                throw new IllegalArgumentException("Unknown value type: " + cd.typeName());
            }
          }
        }
      }
      byte[] result = baos.toByteArray();
      if (result.length > sizeGuess) {
        logger.warn().log("Guessed at {} bytes, but was actually {} bytes", sizeGuess, result.length);
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
  @SuppressFBWarnings(value = {"OBJECT_DESERIALIZATION"}, justification = "Source of bytes is trusted")
  DataRow dataRowDeserializer(byte[] bytes) throws IOException  {
    DataRow result = DataRow.create(types);
    try (ByteArrayInputStream baos = new ByteArrayInputStream(bytes)) {
      try (ObjectInputStream ois = new ObjectInputStream(baos)) {
        for (Iterator<ColumnDefn> iter = types.iterator(); iter.hasNext();) {
          ColumnDefn cd = iter.next();
          if (ois.available() < 1) {
            return result;
          }
          byte typeOrd = ois.readByte();
          if (typeOrd <= 0) {
            result.put(cd.name(), cd.type(), null);
          } else {
            DataType type = DataType.fromOrdinal(typeOrd);
            assert(cd.type() == type);
            switch (type) {
              case DataType.Boolean:
                result.put(cd.name(), cd.type(), ois.readBoolean());
                break;
              case DataType.Date:
                result.put(cd.name(), cd.type(), (LocalDate) ois.readObject());
                break;
              case DataType.DateTime:
                result.put(cd.name(), cd.type(), (LocalDateTime) ois.readObject());
                break;
              case DataType.Double:
                result.put(cd.name(), cd.type(), ois.readDouble());
                break;
              case DataType.Float:
                result.put(cd.name(), cd.type(), ois.readFloat());
                break;
              case DataType.Integer:
                result.put(cd.name(), cd.type(), ois.readInt());
                break;
              case DataType.Long:
                result.put(cd.name(), cd.type(), ois.readLong());
                break;
              case DataType.String:
                int length = ois.readInt();
                byte[] stringBytes = ois.readNBytes(length);
                result.put(cd.name(), cd.type(), new String(stringBytes, StandardCharsets.UTF_8));
                break;
              case DataType.Time:
                result.put(cd.name(), cd.type(), (LocalTime) ois.readObject());
                break;
              default:
                throw new IllegalArgumentException("Unknown value type: " + cd.typeName());
            }
          }
        }
      }
    } catch (ClassNotFoundException ex) {
      logger.error().log("ObjectInputStream threw ClassNotFoundException, which shouldn't happen: ", ex);
      throw new IOException("Unable to deserialize stream", ex);
    }
    return result;
  }

  private String sanitiseSourceName(String name) {
    return name.replaceAll("[\\p{Cntrl}`!\\\"|$%^&*(){}\\[\\];:'@#~,./\\\\<>]*", "_");
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {

    FileSystem fileSystem = vertx.fileSystem();
    String dir = tempDir;
    this.types = input.getTypes();

    return fileSystem.mkdirs(dir)
            .compose(v -> {
              this.stream = new SortingStream<>(pipelineContext
                    , Vertx.currentContext()
                    , fileSystem
                    , new DataRowComparator(pipelineContext, definition.getFields())
                    , this::dataRowSerializer
                    , this::dataRowDeserializer
                    , dir
                    , sanitiseSourceName(getName())
                    , memoryLimit
                    , DataRow::bytesSize
                    , input.getStream()
              );
              return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
            });
  }

}
