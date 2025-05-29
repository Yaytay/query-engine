/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.net.MediaType;
import java.io.IOException;

/**
 * Jackson {@link com.fasterxml.jackson.databind.ser.std.StdSerializer} for writing {@link Double} objects without trailing zeros.
 *
 * @author jtalbut
 */
public class DoubleSerializer extends StdSerializer<Double> {

  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public DoubleSerializer() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param t The class being serialized, which must be {@link MediaType}.
   */
  public DoubleSerializer(Class<Double> t) {
    super(t);
  }

  @Override
  public void serialize(Double value, JsonGenerator gen, SerializerProvider provider) throws IOException {

    if (value % 1 == 0 && Long.MIN_VALUE < value && value < Long.MAX_VALUE) {
      gen.writeNumber(value.longValue()); // Outputs as an integer if no fractional part
    } else {
      gen.writeNumber(value); // Preserves decimal without unnecessary trailing zeros
    }
  }

}
