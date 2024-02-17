/*
 * Copyright (C) 2024 njt
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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SerializeWriteStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(SerializeWriteStreamTest.class);
  
  private static final String OUTPUT_FILE = "target/temp/SerializeWriteStreamTest1.dat";
  
  private final int limit = 200;
  private final int byteMultiplier = 1000;
  private final int smallReadBufferSize = 17;
  private final int largeReadBufferSize = 10000;
  private final Promise<Void> done = Promise.promise();
  private int written = 0;
  private int read = 0;
  private SerializeWriteStream<Integer> sws;
  private SerializeReadStream<Integer> srs;
  private long drains = 0;
  private long startWriteTime;
  private long startReadTime;
  
  @Test
  public void testIntFromByteArray() throws IOException {
    assertEquals(0x01020304, SerializeReadStream.intFromByteArray(new byte[]{1, 2, 3, 4}));
    assertEquals(0xFFFFFFFF, SerializeReadStream.intFromByteArray(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
    
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (DataOutputStream dos = new DataOutputStream(baos)) {
        dos.writeInt(0x12345678);
      }
      byte bytes[] = baos.toByteArray();
      assertEquals(4, bytes.length);
      assertEquals(0x12345678, SerializeReadStream.intFromByteArray(bytes));
    }
  }
  
  private byte[] serialize(Integer i) {
    byte bytes[] = new byte[i * byteMultiplier];
    for (int j = 0; j < bytes.length; j++) {
      bytes[j] = i.byteValue();
    }
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (DataOutputStream dos = new DataOutputStream(baos)) {
        // Note that this has a maximum string length of 64k, which means it's not suitable for my live usage
        dos.writeUTF("Number " + i + ".");
        dos.writeInt(i);
        dos.writeInt(bytes.length);
        dos.write(bytes, 0, bytes.length);                    
      }
      return baos.toByteArray();
    } catch(Throwable ex) {
      throw new RuntimeException("Failed to serialize data " + i, ex);
    }
  }
  
  private Integer deserialize(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      try (DataInputStream dis = new DataInputStream(bais)) {
        // Note that this has a maximum string length of 64k, which means it's not suitable for my live usage
        dis.readUTF();
        int result = dis.readInt();
        int arraySize = dis.readInt();
        byte[] array = new byte[arraySize];
        dis.read(array);
        return result;
      }
    } catch(Throwable ex) {
      throw new RuntimeException("Failed to deserialize " + data.length + " bytes ", ex);
    }
  }
  
  private Future<Void> write() {
    if (written >= limit) {
      done.complete();
      return sws.end();
    }
    if (sws.writeQueueFull()) {
      // logger.debug("Write queue full after {}", written);
      return done.future();
    } else {
      ++written;
      return sws.write(written)
              .compose(v -> {
                return write();
              });
    }    
  }
  
  @Test
  @Order(1)
  public void testWrite(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    fs.open(OUTPUT_FILE, new OpenOptions().setCreate(true).setTruncateExisting(true))
            .compose(af -> {
              sws = new SerializeWriteStream<>(af, this::serialize);
              startWriteTime = System.currentTimeMillis();
              sws.drainHandler(v -> {
                // logger.debug("Draining after {}", written);
                ++drains;
                write();
              });
              return write();
            })
            .onFailure(ex -> {
              logger.debug("Failed after {}ms (including {} drain cycles): ", System.currentTimeMillis() - startWriteTime, drains, ex);
              testContext.failNow(ex);
            })
            .onSuccess(v -> {
              logger.debug("Succeeded after {}ms (including {} drain cycles)", System.currentTimeMillis() - startWriteTime, drains);
              testContext.completeNow();;
            })
            ;
  }
  
  @Test
  @Order(2)
  @Timeout(90000)
  public void testReadOnly(Vertx vertx, VertxTestContext testContext) {
    AtomicLong bytesReceived = new AtomicLong();
    FileSystem fs = vertx.fileSystem();
    fs.open(OUTPUT_FILE, new OpenOptions().setRead(true))
            .compose(af -> {
              af.setReadBufferSize(smallReadBufferSize);
              af.handler(i -> {
                bytesReceived.addAndGet(i.length());
              });
              af.exceptionHandler(ex -> {
                logger.debug("Simple read failed: ", ex);
                testContext.failNow(ex);
              });
              af.endHandler(v -> {
                logger.debug("Simple read succeeded after {}ms", System.currentTimeMillis() - startReadTime);
                testContext.completeNow();
              });
              logger.debug("Starting simple read");
              startReadTime = System.currentTimeMillis();
              af.resume();
              return Future.succeededFuture();
            })
            ;
    
  }

  
  @Test
  @Order(3)
  @Timeout(240000)
  public void testRead(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    read = 0;
    fs.open(OUTPUT_FILE, new OpenOptions().setRead(true))
            .compose(af -> {
              af.setReadBufferSize(smallReadBufferSize);
              srs = new SerializeReadStream<>(af, this::deserialize);
              startReadTime = System.currentTimeMillis();
              srs.handler(i -> {
                logger.debug("Read {}", i);
                ++read;
              });
              srs.exceptionHandler(ex -> {
                logger.debug("Read failed after {}ms: ", System.currentTimeMillis() - startReadTime, ex);
                testContext.failNow(ex);
              });
              srs.endHandler(v -> {
                logger.debug("Read succeeded after {}ms", System.currentTimeMillis() - startReadTime);
                // dumpTracking();
                testContext.verify(() -> {
                  assertEquals(written, read);
                });
                testContext.completeNow();
              });
              srs.resume();
              srs.pause();
              srs.fetch(600);
              try {
                srs.fetch(-1L);
              } catch (IllegalArgumentException ex) {
              }
              srs.fetch(Long.MAX_VALUE);
              srs.fetch(Long.MAX_VALUE);
              return Future.succeededFuture();
            })
            ;
  }
  
  @Test
  @Order(4)
  @Timeout(240000)
  public void testReadWithDelays(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    read = 0;
    AtomicInteger nextFetchSize = new AtomicInteger(0);
    fs.open(OUTPUT_FILE, new OpenOptions().setRead(true))
            .compose(af -> {
              af.setReadBufferSize(largeReadBufferSize);
              srs = new SerializeReadStream<>(af, this::deserialize);
              startReadTime = System.currentTimeMillis();
              srs.handler(i -> {
                logger.debug("Read {}", i);
                ++read;
                vertx.setTimer(200 * nextFetchSize.get(), l -> {
                  srs.fetch(nextFetchSize.incrementAndGet());
                });
              });
              srs.exceptionHandler(ex -> {
                logger.debug("Read failed after {}ms: ", System.currentTimeMillis() - startReadTime, ex);
                testContext.failNow(ex);
              });
              srs.endHandler(v -> {
                logger.debug("Delayed read succeeded after {}ms", System.currentTimeMillis() - startReadTime);
                dumpTracking();
                testContext.verify(() -> {
                  assertEquals(written, read);
                });
                testContext.completeNow();
              });              
              srs.fetch(nextFetchSize.incrementAndGet());
              return Future.succeededFuture();
            })
            ;
    
  }

  
  @Test
  @Order(5)
  @Timeout(240000)
  public void testQuickRead(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    read = 0;
    fs.open(OUTPUT_FILE, new OpenOptions().setRead(true))
            .compose(af -> {
              af.setReadBufferSize(8192);
              srs = new SerializeReadStream<>(af, this::deserialize);
              startReadTime = System.currentTimeMillis();
              srs.handler(i -> {
                logger.debug("Read {}", i);
                ++read;
              });
              srs.exceptionHandler(ex -> {
                logger.debug("Read failed after {}ms: ", System.currentTimeMillis() - startReadTime, ex);
                testContext.failNow(ex);
              });
              srs.endHandler(v -> {
                logger.debug("Quick read succeeded after {}ms", System.currentTimeMillis() - startReadTime);
                dumpTracking();
                testContext.verify(() -> {
                  assertEquals(written, read);
                });
                testContext.completeNow();
              });              
              srs.resume();
              return Future.succeededFuture();
            })
            ;
    
  }
  
  void dumpTracking() {
    try (FileOutputStream strm = new FileOutputStream("target/temp/log.log")) {
      for (int i = 0; i < srs.tracking.size(); ++i) {
        SerializeReadStream.Call call = srs.tracking.get(i);
        StringBuilder sb = new StringBuilder();
        sb.append(call.timestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("\t")
                .append(call.thread())
                .append("\t")
                .append(call.action())
                .append("\t")
                .append(call.indent())
                .append(call.method())
                .append("@")
                .append(call.line())
                .append("\t")
                .append(call.comment() == null ? "" : call.comment())
                .append("\n")
                ;
        strm.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      }
      StringBuilder sb = new StringBuilder();
      sb.append(LocalDateTime.now());
      sb.append("\tBored\n");
      strm.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      logger.warn("Failed to dump log: ", ex);
    }
  }
}
