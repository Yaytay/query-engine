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
package uk.co.spudsoft.query.exec.procs.subquery;

import uk.co.spudsoft.query.exec.procs.subquery.CollatingDestinationInstance;
import io.vertx.core.streams.WriteStream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class CollatingDestinationInstanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(CollatingDestinationInstanceTest.class);
  
  @Test
  public void testGetWriteStream() {
    CollatingDestinationInstance<Class<?>> instance = new CollatingDestinationInstance<>(dr -> dr.getClass());
    WriteStream<DataRow> stream = instance.getWriteStream();
    
    stream.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
    });
    assertTrue(stream.write(null).succeeded());
    assertEquals(stream, stream.setWriteQueueMaxSize(-57));
    assertEquals(stream, stream.drainHandler(v -> {
      throw new IllegalStateException("Bad");
    }));
  }
  
}
