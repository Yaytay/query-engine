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

import io.vertx.core.streams.ReadStream;

/**
 * Tuple of a ReadStream and the Types that represented the fields of the rows it will contain.
 * 
 * Used as the (Future) return value from creators of ReadStreams ({@link SourceInstance} and {@link ProcessorInstance}.
 * 
 * @author jtalbut
 */
public class ReadStreamWithTypes {
  
  private final ReadStream<DataRow> stream;
  private final Types types;

  /**
   * Constructor.
   * @param stream The {@link ReadStream} itself.
   * @param types Get the Types as understood by this ReadStream.
   */
  public ReadStreamWithTypes(ReadStream<DataRow> stream, Types types) {
    this.stream = stream;
    this.types = types;
  }

  /**
   * Get the {@link ReadStream}.
   * @return the {@link ReadStream}.
   */
  public ReadStream<DataRow> getStream() {
    return stream;
  }

  /**
   * Get the Types as understood by this ReadStream.
   * 
   * Note that the types may change as the read stream progresses (those only by the addition of new fields and fields changing from null to a specific type).
   * @return the Types as understood by this ReadStream.
   */
  public Types getTypes() {
    return types;
  }
  
}
