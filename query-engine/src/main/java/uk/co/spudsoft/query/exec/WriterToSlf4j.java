/*
 * Copyright (C) 2023 jtalbut
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

import java.io.IOException;
import java.io.Writer;
import org.slf4j.Logger;

/**
 *
 * @author jtalbut
 */
public class WriterToSlf4j extends Writer {

  private final Logger logger;
  private final String prefix;
  private StringBuffer buffer;

  public WriterToSlf4j(Logger logger, String prefix) {
    this.logger = logger;
    this.prefix = prefix;
  }
  
  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    if (buffer == null) {
      buffer = new StringBuffer();
      buffer.append(prefix);
    }
    buffer.append(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    logger.info(buffer.toString());
  }

  @Override
  public void close() throws IOException {
    buffer = null;
  }
  
}
