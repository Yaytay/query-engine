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
package uk.co.spudsoft.query.exec.fmts.xlsx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A mapper between the Vert.x {@link io.vertx.core.streams.WriteStream} and the JDK {@link java.io.OutputStream}.
 * <P>
 * This is used by the {@link FormatXlsxInstance} because the {@link uk.co.spudsoft.xlsx.XlsxWriter} requires an OutputStream for its input.
 * <P>
 * Note that this class offers a synchronous "close" method, but can only call an asynchronous end() method.
 * This means that it may not be clear when the file has actually closed.
 * In most (HTTP) cases this doesn't matter, but for those cases where it does matter the finalPromise will complete when the file is closed.
 *
 * @author jtalbut
 */
public class OutputWriteStreamWrapper extends OutputStream {

  private final WriteStream<Buffer> outputStream;
  private final Promise<Void> finalPromise;

  /**
   * Constructor.
   * @param outputStream The Vert.x {@link WriteStream that the output is actually written to}.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "OutputWriteStreamWrapper is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public OutputWriteStreamWrapper(WriteStream<Buffer> outputStream) {
    this.outputStream = outputStream;
    this.finalPromise = Promise.promise();
  }

  /**
   * Return true if the {@link WriteStream} is full.
   * @return true if the {@link WriteStream} is full.
   */
  public boolean writeQueueFull() {
    return outputStream.writeQueueFull();
  }

  /**
   * Set the handler to call when the {@link WriteStream} is not full any more.
   * @param handler the handler to call when the {@link WriteStream} is not full any more.
   */
  public void drainHandler(Handler<Void> handler) {
    outputStream.drainHandler(handler);
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(Buffer.buffer(new byte[]{(byte) b}));
  }

  @Override
  public void close() throws IOException {
    outputStream.end(finalPromise);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    Buffer buffer = Buffer.buffer(len);
    buffer.appendBytes(b, off, len);
    outputStream.write(buffer);
  }

  @Override
  public void write(byte[] b) throws IOException {
    outputStream.write(Buffer.buffer(b));
  }


  /**
   * Returns a future that completes when the {@link WriteStream} has reached its final state,
   * i.e., when the stream has been closed or finished writing.
   *
   * @return a {@code Future<Void>} that is completed when the final operation on the stream is done.
   */
  public Future<Void> getFinalFuture() {
    return finalPromise.future();
  }

}
