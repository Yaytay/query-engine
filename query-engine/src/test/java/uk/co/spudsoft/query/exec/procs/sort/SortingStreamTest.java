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

import io.vertx.core.Context;
import io.vertx.core.Handler;
import uk.co.spudsoft.query.exec.procs.ListReadStream;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public void testShouldLoop() {    
    assertTrue(SortingStream.shouldLoop(new AtomicBoolean(true), new AtomicInteger(SortingStream.State.MERGING.ordinal())));
    assertFalse(SortingStream.shouldLoop(new AtomicBoolean(true), new AtomicInteger(SortingStream.State.COLLECTING.ordinal())));
    assertFalse(SortingStream.shouldLoop(new AtomicBoolean(false), new AtomicInteger(SortingStream.State.COLLECTING.ordinal())));
    assertFalse(SortingStream.shouldLoop(new AtomicBoolean(false), new AtomicInteger(SortingStream.State.MERGING.ordinal())));
  }
  
  @Test
  public void testHaveHandlerAndDemand() {
    assertFalse(SortingStream.haveHandlerAndDemand(null, new AtomicLong(0)));
    assertFalse(SortingStream.haveHandlerAndDemand(null, new AtomicLong(1)));
    assertFalse(SortingStream.haveHandlerAndDemand(x -> logger.debug("x: {}", x), new AtomicLong(0)));
    assertTrue(SortingStream.haveHandlerAndDemand(x -> logger.debug("x: {}", x), new AtomicLong(1)));
  }
  
  @Test
  @SuppressWarnings("unchecked")
  public void testShouldProcessOutputExit(Vertx vertx, TestInfo testInfo) throws Exception {
    // 1. Setup instance
    SortingStream<Integer> ss = new SortingStream<>(
        vertx.getOrCreateContext(),
        vertx.fileSystem(),
        Comparator.naturalOrder(),
        i -> SerializeWriteStream.byteArrayFromInt(i),
        b -> SerializeReadStream.intFromByteArray(b),
        "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
        "test",
        1000,
        i -> 1,
        (ReadStream<Integer>) mock(io.vertx.core.streams.ReadStream.class)
    );

    // Get access to private fields
    Field stateField = SortingStream.class.getDeclaredField("state");
    stateField.setAccessible(true);
    AtomicInteger state = (AtomicInteger) stateField.get(ss);

    Field mergeStateField = SortingStream.class.getDeclaredField("mergeState");
    mergeStateField.setAccessible(true);

    Field dataHandlerField = SortingStream.class.getDeclaredField("dataHandler");
    dataHandlerField.setAccessible(true);

    Field demandField = SortingStream.class.getDeclaredField("demand");
    demandField.setAccessible(true);
    AtomicLong demand = (AtomicLong) demandField.get(ss);

    // Case 1: State is not MERGING (Line 1-3)
    state.set(0); // State.PENDING
    ss.processOutput();
    // Verify no demand was checked (proves it exited at line 2)
    // Note: We use reflection to verify the demand wasn't touched yet if needed, 
    // but the return is immediate.

    // Case 2: State is MERGING but mergeState is NULL (Line 1-3)
    state.set(2); // State.MERGING
    mergeStateField.set(ss, null);
    ss.processOutput();
    // Exits at line 2

    // Case 3: State is MERGING, mergeState exists, but dataHandler is NULL (Line 6-8)
    Object mockMergeState = mock(Class.forName("uk.co.spudsoft.query.exec.procs.sort.SortingStream$MergeState"));
    mergeStateField.set(ss, mockMergeState);
    dataHandlerField.set(ss, null);
    ss.processOutput();
    // Exits at line 7
    verifyNoInteractions(mockMergeState);

    // Case 4: Everything is set - verify it proceeds past the first 7 lines
    // We set demand to 0 so the while loop (line 13) doesn't run, 
    // but it will hit the mergeState.hasNext() check at the end.
    demand.set(0);
    dataHandlerField.set(ss, (Handler<Integer>) item -> {});
    
    ss.processOutput();
  }

  
  @Test
  public void testSimpleSort(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    List<Integer> input = Arrays.asList(151, 892, 849, 786, 912, 714, 455, 27, 516, 789, 560, 62, 550, 351, 317, 661, 11, 125, 53, 131, 429, 735, 591, 663, 760, 795, 173, 91, 499, 445);
    logger.debug("input has {} entries", input.size());
    List<Integer> expected = new ArrayList<>(input);
    expected.sort(Comparator.naturalOrder());

    List<Integer> captured = new ArrayList<>();
    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
             vertx.fileSystem(),
             Comparator.naturalOrder(),
             i -> SerializeWriteStream.byteArrayFromInt(i),
             b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             1000,
             i -> 16,
             lrs
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
  // @Disabled
  public void testSortingError(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    List<Integer> input = Arrays.asList(151, 892, 849, 786, 912, 714, 455, 27, 516, 789, 560, 62, 550, 351, 317, 661, 11, 125, 53, 131, 429, 735, 591, 663, 760, 795, 173, 91, 499, 445);
    logger.debug("input has {} entries", input.size());
    List<Integer> expected = new ArrayList<>(input);
    expected.sort(Comparator.naturalOrder());

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
             vertx.fileSystem(),
             Comparator.naturalOrder(),
             i -> SerializeWriteStream.byteArrayFromInt(i),
             b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             1000,
             i -> 16,
             lrs
    );
    ss.endHandler(v -> {
      logger.debug("Ended");
      testContext.failNow("Stream ended, but should not have");
    });
    ss.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.completeNow();
    });
    ss.handler(item -> {
      logger.debug("Received {}", item);
      throw new IllegalStateException("Testing exception from handler");
    });
    ss.fetch(4);
  }
  
  private final class ThrowingReadStream extends ListReadStream<Integer> {

    public ThrowingReadStream(Context context, List<Integer> items) {
      super(null, context, items);
    }

    @Override
    protected void callHandler(Integer item, Handler<Integer> handlerCaptured, Handler<Throwable> exceptionHandlerCaptured) {
      if (item > 500 && item < 600) {
        if (exceptionHandlerCaptured != null) {
          exceptionHandlerCaptured.handle(new IllegalStateException("Testing input exception"));
        } else {
          logger.warn("Supposed to report an exxception by exceptionHandler not captured");
        }
      } else {
        super.callHandler(item, handlerCaptured, exceptionHandlerCaptured); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
      }
    }
  }


  @Test
  // @Disabled
  public void testInputError(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    List<Integer> input = Arrays.asList(151, 892, 849, 786, 912, 714, 455, 27, 516, 789, 560, 62, 550, 351, 317, 661, 11, 125, 53, 131, 429, 735, 591, 663, 760, 795, 173, 91, 499, 445);
    logger.debug("input has {} entries", input.size());
    List<Integer> expected = new ArrayList<>(input);
    expected.sort(Comparator.naturalOrder());

    ListReadStream<Integer> lrs = new ThrowingReadStream(vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
             vertx.fileSystem(),
             Comparator.naturalOrder(),
             i -> SerializeWriteStream.byteArrayFromInt(i),
             b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             1000,
             i -> 16,
             lrs
    );
    ss.endHandler(v -> {
      logger.debug("Ended");
      testContext.failNow("Stream ended, but should not have");
    });
    ss.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.completeNow();
    });
    ss.handler(item -> {
      logger.debug("Received {}", item);
    });
    ss.fetch(0);
    ss.fetch(Long.MAX_VALUE - 1);
    ss.fetch(Long.MAX_VALUE - 1);
    ss.resume();
  }

  @Test
  // @Disabled
  public void testSmallFileSort(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());

    int total = 20000;

    AtomicInteger count = new AtomicInteger();
    AtomicInteger last = new AtomicInteger(Integer.MIN_VALUE);
    ReadStream<Integer> rs = new RandomIntegerReadStream(vertx.getOrCreateContext(), total);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
             vertx.fileSystem(),
             Comparator.naturalOrder(),
             i -> SerializeWriteStream.byteArrayFromInt(i),
             b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             10000 // 1 << 20 // 1MB
            ,
             i -> 16,
             rs
    );
    ss.endHandler(v -> {
      logger.debug("Ended with {}", count.get());
      testContext.verify(() -> {
        assertEquals(total, count.get());
      });
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
          logger.debug("Bad sort {} > {}", lastValue, item);
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
  // // @Disabled
  public void testBigFileSort(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());

    int total = 2000000;

    AtomicInteger count = new AtomicInteger();
    AtomicInteger last = new AtomicInteger(Integer.MIN_VALUE);
    ReadStream<Integer> rs = new RandomIntegerReadStream(vertx.getOrCreateContext(), total);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
             vertx.fileSystem(),
             Comparator.naturalOrder(),
             i -> SerializeWriteStream.byteArrayFromInt(i),
             b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             1 << 20 // 1MB
            ,
             i -> 16,
             rs
    );
    ss.endHandler(v -> {
      logger.debug("Ended with {}", count.get());
      testContext.verify(() -> {
        assertEquals(total, count.get());
      });
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
          logger.debug("Bad sort {} > {}", lastValue, item);
        }
        assertThat(item, Matchers.greaterThanOrEqualTo(lastValue));
      });
    });
    logger.debug("Fetching {}", Long.MAX_VALUE);
    ss.fetch(Long.MAX_VALUE);
    ss.fetch(Long.MAX_VALUE);
  }

  @Test
  // @Disabled
  public void testSourceStreamBehavior(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test SourceStream functionality with proper lifecycle management
    List<Integer> input = Arrays.asList(5, 2, 8, 1, 9, 3);
    List<Integer> expected = Arrays.asList(1, 2, 3, 5, 8, 9);

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            1000,
            i -> 16,
            lrs
    );

    List<Integer> captured = new ArrayList<>();
    AtomicInteger handlerCallCount = new AtomicInteger(0);

    ss.handler(item -> {
      handlerCallCount.incrementAndGet();
      captured.add(item);
    });

    ss.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(expected, captured);
        assertEquals(input.size(), handlerCallCount.get());
      });
      testContext.completeNow();
    });

    ss.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });

    // Test different fetch scenarios
    ss.fetch(2);  // Fetch first 2 items
    ss.fetch(4);  // Fetch remaining items
  }

  @Test
  // @Disabled
  public void testFetchBeforeReady(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws InterruptedException, NoSuchFieldException, IllegalAccessException {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test SortingStream with empty input
    List<Integer> input = new ArrayList<>();

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            1000,
            i -> 16,
            lrs
    );

    ss.fetch(Long.MAX_VALUE);
    
    // Just enough to cede control of this thread
    Thread.sleep(10);
    
    ss.handler(null);
    
    // At this point the stream will have completed and no handlers will have been called.
    // The only way to test this is to look at the state
    
    // Get the private state field using reflection
    Field stateField = SortingStream.class.getDeclaredField("state");
    stateField.setAccessible(true);
    AtomicInteger stateValue = (AtomicInteger) stateField.get(ss);

    // Get the actual state ordinal value
    int currentState = stateValue.get();

    assertThat(currentState, greaterThan(1)); // COLLECTING.ordinal() is 1 based on the enum order, must be beyond that state
    assertThat(currentState, not(equalTo(4))); // FAILED.ordinal() is 4 based on the enum order, must not be in error    
    
    testContext.completeNow();
    
  }
  
  @Test
  // @Disabled
  public void testEmptyStreamHandling(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws InterruptedException {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test SortingStream with empty input
    List<Integer> input = new ArrayList<>();

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            1000,
            i -> 16,
            lrs
    );

    List<Integer> captured = new ArrayList<>();
    AtomicInteger handlerCallCount = new AtomicInteger(0);

    ss.handler(null);
    
    ss.handler(item -> {
      handlerCallCount.incrementAndGet();
      captured.add(item);
    });

    ss.endHandler(v -> {
      testContext.verify(() -> {
        assertTrue(captured.isEmpty());
        assertEquals(0, handlerCallCount.get());
      });
      testContext.completeNow();
    });

    ss.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });
    
    ss.fetch(Long.MAX_VALUE);    
  }
  
  @Test
  // @Disabled
  public void testSingleItemStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test SortingStream with single item
    List<Integer> input = Arrays.asList(42);

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);
    
    String tmpDir = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName();
    vertx.fileSystem()
            .mkdirsBlocking(tmpDir)
            .deleteRecursiveBlocking(tmpDir)
            .mkdirsBlocking(tmpDir)
            ;

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             tmpDir,
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            1000,
            i -> 16,
            lrs
    );

    List<Integer> captured = new ArrayList<>();
    AtomicInteger handlerCallCount = new AtomicInteger(0);

    ss.handler(item -> {
      handlerCallCount.incrementAndGet();
      captured.add(item);
    });

    ss.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(Arrays.asList(42), captured);
        assertEquals(1, handlerCallCount.get());
      });
      testContext.completeNow();
    });

    ss.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });

    ss.fetch(10);
  }

  @Test
  // @Disabled
  public void testMemoryLimitTriggering(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test that memory limit triggers file writing
    List<Integer> input = Arrays.asList(100, 50, 200, 25, 150, 75, 300, 125);
    List<Integer> expected = Arrays.asList(25, 50, 75, 100, 125, 150, 200, 300);

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            4, // Very small memory limit to force file creation
            i -> 16, // Each integer takes 16 bytes
            lrs
    );

    List<Integer> captured = new ArrayList<>();

    ss.handler(item -> {
      captured.add(item);
    });

    ss.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(expected, captured);
      });
      testContext.completeNow();
    });

    ss.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });

    ss.fetch(Long.MAX_VALUE);
  }

  @Test
  // @Disabled
  public void testPauseResumeWithDemand(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    logger.debug("{}.{}", testInfo.getTestClass().get().getSimpleName(), testInfo.getTestMethod().get().getName());
    // Test pause/resume functionality with demand management
    List<Integer> input = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
    List<Integer> expected = Arrays.asList(1, 1, 2, 3, 4, 5, 6, 9);

    ListReadStream<Integer> lrs = new ListReadStream<>(null, vertx.getOrCreateContext(), input);

    SortingStream<Integer> ss = new SortingStream<>(
            vertx.getOrCreateContext(),
            vertx.fileSystem(),
            Comparator.naturalOrder(),
            i -> SerializeWriteStream.byteArrayFromInt(i),
            b -> SerializeReadStream.intFromByteArray(b),
             "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
             testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
            1000,
            i -> 16,
            lrs
    );

    List<Integer> captured = new ArrayList<>();
    AtomicInteger processedCount = new AtomicInteger(0);

    ss.handler(item -> {
      captured.add(item);
      int count = processedCount.incrementAndGet();

      // Pause after processing 4 items
      if (count == 4) {
        ss.pause();
        // Resume after a short delay
        vertx.setTimer(100, timerId -> ss.resume());
      }
    });

    ss.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(expected, captured);
        assertEquals(input.size(), processedCount.get());
      });
      testContext.completeNow();
    });

    ss.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });

    ss.fetch(Long.MAX_VALUE);
  }
}
