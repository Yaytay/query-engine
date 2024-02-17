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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.streams.ReadStream;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 * @param <T> The type of object that will be read from the file.
 */
public final class SerializeReadStream<T> implements ReadStream<T> {
  
  private static final Logger logger = LoggerFactory.getLogger(SerializeReadStream.class);
  
  private final AsyncFile file;
  private final Function<byte[], T> deserializer;
  private final Object lock = new Object();
  private final Context context;
  
  // Standard fields required to implement ReadStream
  private Handler<Throwable> exceptionHandler;
  private Handler<T> itemHandler;
  private Handler<Void> endHandler;
  private long demand;

  // Buffers handled from the AsyncFile
  private Deque<Buffer> buffers = new ArrayDeque<>();
  
  // Bytes read from current head Buffer
  private int readPos;
  
  // True when the file has been read completely
  private boolean ended;
  private boolean endHandlerCalled;

  private long totalBytesReceived = 0;
  private enum Action { Enter, Exit, Throw };
  public record Call (LocalDateTime timestamp, String thread, String indent, String action, String method, int line, String comment) {};
  private final Object trackLock = new Object();
  public ArrayList<Call> tracking = new ArrayList<>();
  private final StringBuilder indent = new StringBuilder();
  private void track(Action action, String comment) {
    StackTraceElement caller = Thread.currentThread().getStackTrace()[2];    
    if (action == Action.Enter) {
      indent.append("  ");
    }
    Call call = new Call(
            LocalDateTime.now()
            , Thread.currentThread().getName()
            , indent.toString()
            , action.name()
            , caller.getMethodName()
            , caller.getLineNumber()
            , comment
    );
    if (action != Action.Enter) {
      indent.delete(0, 2);
    }
    synchronized(trackLock) {
      tracking.add(call);
    }
  }
  
  static int intFromByteArray(byte[] bytes) {
    return 
            ((bytes[0] & 0xFF) << 24)
          | ((bytes[1] & 0xFF) << 16) 
          | ((bytes[2] & 0xFF) << 8)
          |  (bytes[3] & 0xFF) 
            ;
  }
  
  private void shuffleBuffer() {
    int bufferCount;
    synchronized (lock) {
      if (!buffers.isEmpty()) {
        Buffer headBuffer = buffers.getFirst();
        track(Action.Enter, "readPos: " + readPos + "; headBuffer: " + headBuffer.length());
        boolean removed = false;
        if (readPos >= headBuffer.length()) {
          buffers.removeFirst();
          readPos = 0;
          removed = true;
        }
        track(Action.Exit, removed ? "removed" : null);
      }
      bufferCount = buffers.size();
    }
    if (bufferCount < 2 && demand < Long.MAX_VALUE) {
      file.fetch(4);
    }
  }
  
  /**
   * Constructor.
   * 
   * The AsyncFile should have been opened with the "read" OpenOption and a suitable readBufferSize.
   * The readBufferSize should be large to reduce the number of round trips to the file, but small enough to avoid wasting memory.
   * Aim for a single read buffer to account for a few T objects if possible.
   * 
   * The deserializer will always be called with a byte array that contains a single T object (as written by the serailizer 
   * passed in to a SerializeWriteStream).
   * 
   * @param file The AsyncFile that is going to be read.
   * @param deserializer Function to convert a byte buffer to a single T object.
   * 
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SerializeReadStream(AsyncFile file, Function<byte[], T> deserializer) {
    track(Action.Enter, null);
    this.file = file;
    this.deserializer = deserializer;
    this.context = Vertx.currentContext();
    file.handler(this::handle);
    file.endHandler(v -> {
      logger.trace("File input ended");
      synchronized (lock) {
        ended = true;
      }
      context.runOnContext(this::process);
    });
    
    // Initialize processing
    currentRequirement = 4;
    currentByteArray = new byte[4];
    gettingSize = true;
    
    track(Action.Exit, null);
  }
  
  private void handleException(Throwable ex) {
    track(Action.Enter, null);
    if (exceptionHandler != null) {
      exceptionHandler.handle(ex);
    }
    track(Action.Exit, null);
  }
  
  // Array of bytes currently being filled, never gets smaller
  private byte[] currentByteArray;
  private int currentRequirement;
  private int bytesWrittenToCurrentByteArray;
  private boolean gettingSize;
  
  /**
   * Worker that must be run on context and that does whatever is necessary,
   * 
   * Loops until no more work is possible.
   */
  private void process(Void v) {
    track(Action.Enter, null);
    
    int iterations = 0;
    while (true) {
      Buffer headBuffer = null;
      Handler<Void> endHandlerCaptured = null;
      boolean endHandlerCalledCaptured = false;
      long demandCaptured;
      synchronized (lock) {
        demandCaptured = demand;
        endHandlerCalledCaptured = endHandlerCalled;
        if (buffers.isEmpty()) {
          if (ended) {
            endHandlerCaptured = endHandler;
            endHandlerCalled = true;
          }
        } else {
          headBuffer = buffers.getFirst();
        }
      }
      if (endHandlerCaptured != null) {
        if (!endHandlerCalledCaptured) {
          endHandlerCaptured.handle(null);
          track(Action.Exit, "Ended");
        } else {
          track(Action.Exit, "Ended already");
        }
        return ;
      }
      if (headBuffer == null) {
        // Not at the end, and got no data to process
        track(Action.Exit, "Out of buffers after " + iterations + " iterations; demand: " + demandCaptured);
        return ;
      }
      if (demandCaptured <= 0) {
        // Currently suspended
        track(Action.Exit, "Demand is zero after " + iterations + " iterations; demand: " + demandCaptured);
        return ;
      }
      processOneCycle(headBuffer);
      ++iterations;
    }    
  }

  private boolean processOneCycle(Buffer headBuffer) {
    track(Action.Enter, null);
    int remaining = currentRequirement - bytesWrittenToCurrentByteArray;
    int toRead = remaining; // Assume that the entire (or remaining) requirement can be satisfied from the head buffer
    if (headBuffer.length() - readPos < remaining) {
      // Only some of the data can be got from the current head buffer
      toRead = headBuffer.length() - readPos;
    }
    headBuffer.getBytes(readPos, readPos + toRead, currentByteArray, bytesWrittenToCurrentByteArray);
    bytesWrittenToCurrentByteArray += toRead;
    readPos += toRead;
    shuffleBuffer();
    if (bytesWrittenToCurrentByteArray >= currentRequirement) {
      // currentByteArray is full
      if (gettingSize) {
        currentRequirement = intFromByteArray(currentByteArray);
        gettingSize = false;
        if (currentRequirement > currentByteArray.length) {
          currentByteArray = new byte[currentRequirement];
        }
        bytesWrittenToCurrentByteArray = 0;
      } else {
        passToReader(currentByteArray);
        gettingSize = true;
        currentRequirement = 4;
        bytesWrittenToCurrentByteArray = 0;
      }
    }
    track(Action.Exit, null);
    return true;
  }
  
  private void passToReader(byte[] bytes) {
    track(Action.Enter, null);
    T item = null;
    try {
      item = deserializer.apply(bytes);
      itemHandler.handle(item);
    } catch (Throwable ex) {
      track(Action.Throw, ex.toString());
      handleException(ex);
    }        
    synchronized (lock) {
      if (demand < Long.MAX_VALUE) {
        --demand;
      }
      track(Action.Exit, item == null ? null : item.toString());
    }
  }
  
  private void handle(Buffer buffer) {
    logger.trace("Handler {} bytes", buffer.length());
    totalBytesReceived += buffer.length();
    track(Action.Enter, "Buffers: " + buffers.size() + "; ReadPos: " + readPos + "; "  + "; bytesWrittenToCurrentByteArray: " + bytesWrittenToCurrentByteArray + "; currentRequirement: " + currentRequirement + "; totalBytesReceived: " + totalBytesReceived);
    int bufferCount;
    synchronized (lock) {
      buffers.add(buffer);
      bufferCount = buffers.size();
    }
    // Queue up some buffers, but not too many
    if (bufferCount > 4) {
      file.pause();
    } else if (bufferCount <= 0) {
      file.fetch(2);
    }
    context.runOnContext(v -> {
      process(null);
    });
    track(Action.Exit, "Buffers: " + buffers.size() + "; ReadPos: " + readPos + "; "  + "; bytesWrittenToCurrentByteArray: " + bytesWrittenToCurrentByteArray + "; currentRequirement: " + currentRequirement);
  }
  
  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    file.exceptionHandler(handler);
    exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.itemHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }
  
  @Override
  public ReadStream<T> pause() {
    track(Action.Enter, null);    
    synchronized (lock) {
      this.demand = 0;
    }
    track(Action.Exit, null);    
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    track(Action.Enter, null);    
    synchronized (lock) {
      this.demand = Integer.MAX_VALUE;
    }
    file.resume();
    context.runOnContext(v -> {
      process(null);
    });
    track(Action.Exit, null);    
    return this;
  }

  @Override
  public ReadStream<T> fetch(long amount) {
    track(Action.Enter, Long.toString(amount));
    if (amount < 0L) {
      track(Action.Throw, null);    
      throw new IllegalArgumentException();
    }
    synchronized (lock) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
    }
    context.runOnContext(v -> {
      process(null);
    });
    file.fetch(2);
    track(Action.Exit, Long.toString(demand));    
    return this;
  }

}
