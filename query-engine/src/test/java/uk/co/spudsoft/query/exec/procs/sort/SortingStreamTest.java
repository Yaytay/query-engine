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

import uk.co.spudsoft.query.exec.procs.ListReadStream;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.procs.sort.SortingStream.SourceStream;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SortingStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(SortingStreamTest.class);
  
  @Test
  public void testSourceStreamEquals(Vertx vertx) {
    SortingStream<Integer> ss = new SortingStream<>(null, null, null, null, null, null, null, 1, null, new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(2)));
    SortingStream<Integer>.SourceStream strm1 = ss.new SourceStream(null, 1, new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(2)));
    SortingStream<Integer>.SourceStream strm2 = ss.new SourceStream(null, 2, new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(2)));
    assertTrue(strm1.equals(strm1));
    assertFalse(strm1.equals(null));
    assertFalse(strm1.equals("no"));
    assertFalse(strm1.equals(strm2));
  }
  
  @Test
  public void testSimpleSort(Vertx vertx, VertxTestContext testContext) {    
    List<Integer> input = Arrays.asList(151, 892, 849, 786, 912, 714, 455, 27, 516, 789, 560, 62, 550, 351, 317, 661, 11, 125, 53, 131, 429, 735, 591, 663, 760, 795, 173, 91, 499, 445);
    logger.debug("input has {} entries", input.size());
    List<Integer> expected = new ArrayList<>(input);
    expected.sort(Comparator.naturalOrder());
    
    List<Integer> captured = new ArrayList<>();
    ListReadStream<Integer> lrs = new ListReadStream<>(vertx.getOrCreateContext(), input);
    
    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext()
            , vertx.fileSystem()
            , Comparator.naturalOrder()
            , i -> SerializeWriteStream.byteArrayFromInt(i)
            , b -> SerializeReadStream.intFromByteArray(b)
            , null
            , "SortingStreamTest_testSimpleSort"
            , 1000
            , i -> 16
            , lrs
    );
    ss.endHandler(v -> {
      logger.debug("Ended");
      testContext.verify(() -> {
        assertEquals(expected, captured);
      });
      testContext.completeNow();
    });
    ss.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.failNow(ex);
    });
    ss.handler(item -> {
      logger.debug("Received {}", item);
      captured.add(item);
    });
    logger.debug("Fetching 1");
    ss.fetch(1);
    logger.debug("Pausing");
    ss.pause();
    logger.debug("Resuming");
    ss.resume();
  }

  @Test
  @Timeout(24000000)
  public void testSmallFileSort(Vertx vertx, VertxTestContext testContext) {
    
    int total = 20000;
    
    AtomicInteger count = new AtomicInteger();
    AtomicInteger last = new AtomicInteger(Integer.MIN_VALUE);
    ReadStream<Integer> rs = new RandomIntegerReadStream(vertx.getOrCreateContext(), total);
    
    String tempDir = vertx.fileSystem().createTempDirectoryBlocking("SortingStreamTest_testSmallFileSort");
    
    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext()
            , vertx.fileSystem()
            , Comparator.naturalOrder()
            , i -> SerializeWriteStream.byteArrayFromInt(i)
            , b -> SerializeReadStream.intFromByteArray(b)
            , tempDir
            , "SortingStreamTest_testSmallFileSort"
            , 10000 // 1 << 20 // 1MB
            , i -> 16
            , rs
    );
    ss.endHandler(v -> {
      logger.debug("Ended with {}", count.get());
      testContext.verify(() -> {
        assertEquals(total, count.get());
      });
      vertx.fileSystem().deleteRecursiveBlocking(tempDir);
      testContext.completeNow();
    });
    ss.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.failNow(ex);
    });
    ss.handler(item -> {
      count.incrementAndGet();
      // logger.debug("Received {} ({} so far)", item, count.get());
      testContext.verify(() -> {
        Integer lastValue = last.getAndSet(item);
        if (lastValue > item) {
          logger.debug("Bad sort");
        }
        assertThat(item, Matchers.greaterThanOrEqualTo(lastValue));
      });
    });    
    assertThrows(IllegalArgumentException.class, () -> {
      ss.fetch(-1);
    });
    logger.debug("Fetching {}", total + 10);
    ss.fetch(total + 10);
  }

  @Test
  @Timeout(240000)
  public void testBigFileSort(Vertx vertx, VertxTestContext testContext) {
    
    int total = 2000000;
    
    AtomicInteger count = new AtomicInteger();
    AtomicInteger last = new AtomicInteger(Integer.MIN_VALUE);
    ReadStream<Integer> rs = new RandomIntegerReadStream(vertx.getOrCreateContext(), total);
    
    String tempDir = vertx.fileSystem().createTempDirectoryBlocking("SortingStreamTest_testBigFileSort");
    
    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext()
            , vertx.fileSystem()
            , Comparator.naturalOrder()
            , i -> SerializeWriteStream.byteArrayFromInt(i)
            , b -> SerializeReadStream.intFromByteArray(b)
            , tempDir
            , "SortingStreamTest_testBigFileSort"
            , 1 << 20 // 1MB
            , i -> 16
            , rs
    );
    ss.endHandler(v -> {
      logger.debug("Ended with {}", count.get());
      testContext.verify(() -> {
        assertEquals(total, count.get());
      });
      vertx.fileSystem().deleteRecursiveBlocking(tempDir);
      testContext.completeNow();
    });
    ss.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.failNow(ex);
    });
    ss.handler(item -> {
      count.incrementAndGet();
      // logger.debug("Received {} ({} so far)", item, count.get());
      testContext.verify(() -> {
        Integer lastValue = last.getAndSet(item);
        if (lastValue > item) {
          logger.debug("Bad sort");
        }
        assertThat(item, Matchers.greaterThanOrEqualTo(lastValue));
      });
    });
    logger.debug("Fetching {}", Long.MAX_VALUE);
    ss.fetch(Long.MAX_VALUE);
    ss.fetch(Long.MAX_VALUE);
  }

}
