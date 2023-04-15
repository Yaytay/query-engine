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
package uk.co.spudsoft.query.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.streams.WriteStream;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.DataRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 *
 * @author jtalbut
 */
public class ProcessorDestinationTest {
  
  @Test
  public void testInitialize() {
    ProcessorFormat instance = new ProcessorFormat(null);
    // Initialize is a noop
    assertEquals(Future.succeededFuture(), instance.initialize(null, null));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetWriteStream() {
    WriteStream<DataRow> stream = mock(WriteStream.class);
    ProcessorFormat instance = new ProcessorFormat(stream);
    assertSame(stream, instance.getWriteStream());
  }
  
}
