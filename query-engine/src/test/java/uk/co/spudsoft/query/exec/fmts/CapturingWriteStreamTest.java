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
package uk.co.spudsoft.query.exec.fmts;

import io.vertx.core.Promise;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.DataRow;

/**
 *
 * @author jtalbut
 */
public class CapturingWriteStreamTest {
  
  /**
   * Test of exceptionHandler method, of class CapturingWriteStream.
   */
  @Test
  public void testExceptionHandler() {
    Promise<List<DataRow>> finalPromise = Promise.promise();
    CapturingWriteStream capturingStream = new CapturingWriteStream(
            rows -> {
              finalPromise.tryComplete(rows);
            }
    );
    // Noop
    capturingStream.exceptionHandler(ex -> {
    });
  }

  /**
   * Test of setWriteQueueMaxSize method, of class CapturingWriteStream.
   */
  @Test
  public void testSetWriteQueueMaxSize() {
    Promise<List<DataRow>> finalPromise = Promise.promise();
    CapturingWriteStream capturingStream = new CapturingWriteStream(
            rows -> {
              finalPromise.tryComplete(rows);
            }
    );
    assertThrows(UnsupportedOperationException.class, () -> {
      capturingStream.setWriteQueueMaxSize(4);
    });
  }

  /**
   * Test of drainHandler method, of class CapturingWriteStream.
   */
  @Test
  public void testDrainHandler() {
    Promise<List<DataRow>> finalPromise = Promise.promise();
    CapturingWriteStream capturingStream = new CapturingWriteStream(
            rows -> {
              finalPromise.tryComplete(rows);
            }
    );
    assertThrows(UnsupportedOperationException.class, () -> {
      capturingStream.drainHandler(v -> {});
    });
  }
  
}
