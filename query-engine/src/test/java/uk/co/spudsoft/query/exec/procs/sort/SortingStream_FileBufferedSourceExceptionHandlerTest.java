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
package uk.co.spudsoft.query.exec.procs.sort;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

@ExtendWith(VertxExtension.class)
public class SortingStream_FileBufferedSourceExceptionHandlerTest {

  @Test
  public void testFileBufferedSourceExceptionHandler(Vertx vertx, VertxTestContext testContext, @TempDir Path tempDir) throws IOException {
    Context context = vertx.getOrCreateContext();
    FileSystem fileSystem = vertx.fileSystem();
    
    // Create a temporary file with serialized data
    Path tempFile = Files.createTempFile(tempDir, "test", ".tmp");
    
    // Write some test data to the file
    List<String> testData = Arrays.asList("apple", "banana", "cherry");
    SerializeWriteStream.Serializer<String> serializer = String::getBytes;
    
    fileSystem.open(tempFile.toString(), new io.vertx.core.file.OpenOptions().setCreate(true).setWrite(true))
        .compose(file -> {
          SerializeWriteStream<String> writeStream = new SerializeWriteStream<>(file, serializer);
          return writeAllItems(writeStream, testData);
        })
        .compose(v -> {
          List<String> inputData = new ArrayList<>(20);
          for (int i = 0; i < 20; ++i) {
            inputData.add("item " + i);
          }
          // Now create a SortingStream that will use file-based merging
          ReadStream<String> inputStream = new ListReadStream<>(null, vertx.getOrCreateContext(), inputData);
          
          SortingStream<String> sortingStream = new SortingStream<>(
              context,
              fileSystem,
              Comparator.naturalOrder(),
              serializer,
              this::createFailingDeserializer, // This will cause the exception
              tempDir.toString(),
              "test",
              10, // Small memory limit to force file usage
              String::length,
              inputStream
          );
          
          AtomicReference<Throwable> caughtException = new AtomicReference<>();
          
          sortingStream.exceptionHandler(ex -> {
            caughtException.set(ex);
            testContext.verify(() -> {
              assertNotNull(ex);
              // Verify that the exception came from the deserializer
              assertTrue(ex instanceof RuntimeException);
              assertEquals("Deserializer test exception", ex.getMessage());
            });
            testContext.completeNow();
          });
          
          // Set up handlers to trigger the merge phase
          sortingStream.handler(item -> {
            // This should not be called due to the exception
            testContext.failNow("Should not receive data due to exception");
          });
          
          sortingStream.endHandler(v2 -> {
            // This should not be called due to the exception
            testContext.failNow("Should not complete due to exception");
          });
          
          // Start the stream
          sortingStream.resume();
          
          return Future.succeededFuture();
        })
        .onFailure(testContext::failNow);
  }
    
  private String createFailingDeserializer(byte[] data) {
    // This deserializer will throw an exception when called
    throw new RuntimeException("Deserializer test exception");
  }
  
  private Future<Void> writeAllItems(SerializeWriteStream<String> stream, List<String> items) {
    // Simple implementation to write all items
    return Future.succeededFuture()
        .compose(v -> {
          for (String item : items) {
            stream.write(item);
          }
          return stream.end();
        });
  }
}