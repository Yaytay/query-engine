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
package uk.co.spudsoft.query.defn;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The data type of arguments to pipelines.
 * @author jtalbut
 */
@Schema(description = """
                      <P>The data type of an Argument.</P>
                      <P>All arguments will be received as Strings and will be parsed to the relavant type.</P>
                      """)
public enum ArgumentType {
  /**
   * The argument is something that can be stored in a Java String.
   */
  @Schema(description = "The argument is something that can be stored in a Java String.")
  String,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Integer#parseInt(java.lang.String)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.lang.Integer#parseInt(java.lang.String).")
  Integer,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Long#parseLong(java.lang.String)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.lang.Long#parseLong(java.lang.String).")
  Long,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Double#parseDouble(java.lang.String)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.lang.Double#parseDouble(java.lang.String).")
  Double,
  /**
   * The argument must be something that can be parsed from a String using {@link java.time.LocalDate#parse(java.lang.CharSequence)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.time.LocalDate#parse(java.lang.CharSequence).")
  Date,
  /**
   * The argument must be something that can be parsed from a String using {@link java.time.LocalTime#parse(java.lang.CharSequence)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.time.LocalTime#parse(java.lang.CharSequence).")
  Time,
  /**
   * The argument must be something that can be parsed from a String using {@link java.time.LocalDateTime#parse(java.lang.CharSequence)}.
   */
  @Schema(description = "The argument must be something that can be parsed from a String using java.time.LocalDateTime#parse(java.lang.CharSequence).")
  DateTime
}
