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
package uk.co.spudsoft.query.exec;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class CachingWriteStreamTest {

  private static final Logger logger = LoggerFactory.getLogger(CachingWriteStreamTest.class);
  
  @RegisterExtension
  RunTestOnContext rtoc = new RunTestOnContext();
  
  @Test
  public void testCacheStream(VertxTestContext testContext) {
    Vertx vertx = rtoc.vertx();
    
    List<Buffer> list = new ArrayList<>(1000);
    for (int i = 0; i < 1000; ++i) {
      list.add(Buffer.buffer(Integer.toString(i) + "\n"));
    }
    assertEquals(1000, list.size());
    ListReadStream<Buffer> input = new ListReadStream<>(null, vertx.getOrCreateContext(), list);
    
    String outputFile = "target/temp/CachingWriteStreamTest.txt";
    
    vertx.fileSystem().mkdirsBlocking("target/temp");
    try {
      vertx.fileSystem().deleteBlocking(outputFile);
    } catch(Throwable ex) {
    }
    
    List<Buffer> out = new ArrayList<>(1002);
    WriteStream<Buffer> output = new ListingWriteStream<>(out);
    
    CachingWriteStream.cacheStream(vertx, output, outputFile)            
            .compose(cws -> {
              cws.exceptionHandler(ex -> {
                testContext.failNow(ex);
              });
              cws.setWriteQueueMaxSize(2);
              return input.pipeTo(cws);
            })
            .onSuccess(v -> {
              testContext.verify(() -> {
                assertEquals(list.size(), out.size());
                assertEquals(list, out);
                
                List<String> lines = new ArrayList<>(1003);
                try (FileReader fr = new FileReader(outputFile)) {
                  try (BufferedReader br = new BufferedReader(fr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                      lines.add(line);
                    }
                  }
                }
                assertEquals(lines.size(), out.size());
                for (int i = 0; i < list.size(); ++i) {
                  assertEquals(lines.get(i), list.get(i).toString(StandardCharsets.UTF_8).trim());
                }
              });
              testContext.completeNow();
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            });
    
  }
    
  public class ThrowingWriteStream<T> implements WriteStream<T> {

    private int count;
    private final int max;
    private Handler<Throwable> exceptionHandler;

    public ThrowingWriteStream(int max) {
      this.max = max;
    }
    
    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(T data) {
      if (++count > max) {
        exceptionHandler.handle(new IllegalStateException("Write has been called  " + count + " times"));
        return Future.failedFuture("Write has been called  " + count + " times");
      } 
      return Future.succeededFuture();
    }

    @Override
    public Future<Void> end() {
      return Future.succeededFuture();
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
      return this;
    }

    @Override
    public boolean writeQueueFull() {
      return false;
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
      return this;
    }

  }
  
  @Test
  public void testCacheStreamWithOutputError(VertxTestContext testContext) {
    Vertx vertx = rtoc.vertx();
    
    List<Buffer> list = new ArrayList<>(1000);
    for (int i = 0; i < 1000; ++i) {
      list.add(Buffer.buffer(Integer.toString(i) + "\n"));
    }
    assertEquals(1000, list.size());
    ListReadStream<Buffer> input = new ListReadStream<>(null, vertx.getOrCreateContext(), list);
    
    String outputFile = "target/temp/CachingWriteStreamTest.testCacheStreamWithOutputError.txt";
    
    vertx.fileSystem().mkdirsBlocking("target/temp");
    try {
      vertx.fileSystem().deleteBlocking(outputFile);
    } catch(Throwable ex) {
    }
    
    WriteStream<Buffer> output = new ThrowingWriteStream<>(7);
    
    CachingWriteStream.cacheStream(vertx, output, outputFile)            
            .compose(cws -> {
              cws.exceptionHandler(ex -> {
                logger.info("Exception handler called");
                testContext.completeNow();
              });
              cws.setWriteQueueMaxSize(2);
              return input.pipeTo(cws);
            })
            .onSuccess(v -> {
              testContext.failNow("Pipe should not have succeeded");
            })
            .onFailure(ex -> {
              // Wait half a second to give it time to delete the file
              vertx.timer(500).andThen(ar -> {
                testContext.verify(() -> {
                  assertFalse(vertx.fileSystem().existsBlocking(outputFile));
                });
                testContext.completeNow();
              });
            });
    
  }
  
  @Test
  public void testCacheStreamWithCacheError(VertxTestContext testContext) {
    Vertx vertx = rtoc.vertx();
    
    List<Buffer> list = new ArrayList<>(1000);
    for (int i = 0; i < 1000; ++i) {
      list.add(Buffer.buffer(Integer.toString(i) + "\n"));
    }
    assertEquals(1000, list.size());
    ListReadStream<Buffer> input = new ListReadStream<>(null, vertx.getOrCreateContext(), list);
    
    String outputFile = "target/temp/CachingWriteStreamTest.testCacheStreamWithCacheError.txt";
    
    vertx.fileSystem().mkdirsBlocking("target/temp");
    try {
      vertx.fileSystem().deleteBlocking(outputFile);
    } catch(Throwable ex) {
    }
    vertx.fileSystem().writeFileBlocking(outputFile, Buffer.buffer("Hello"));

    testContext.verify(() -> {
      assertTrue(vertx.fileSystem().existsBlocking(outputFile));
    });
    
    List<Buffer> out = new ArrayList<>(1002);
    WriteStream<Buffer> output = new ListingWriteStream<>(out);
    
    CachingWriteStream cws = new CachingWriteStream(vertx.fileSystem(), outputFile, new ThrowingWriteStream<>(13), output);
    cws.exceptionHandler(ex -> {
      logger.info("Exception handler called and should not have been");
      testContext.failNow(ex);
    });
    cws.setWriteQueueMaxSize(2);
    input.pipeTo(cws)
            .onSuccess(v -> {
              // Wait half a second to give it time to delete the file
              vertx.timer(500).andThen(ar -> {
                testContext.verify(() -> {
                  assertEquals(list.size(), out.size());
                  assertEquals(list, out);
                  assertFalse(vertx.fileSystem().existsBlocking(outputFile));
                });
                testContext.completeNow();
              });
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            });
    
  }
}
