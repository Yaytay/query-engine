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
 * The type of Source.
 * @author jtalbut
 */
@Schema(description = """
                      The type of Source.
                      """)
public enum SourceType {
  /**
   * The test source that outputs a fixed name and a sequence of numbers.
   */
  TEST, 
  /**
   * The SQL source that pulls data from a database.
   */
  SQL, 
  /**
   * The HTTP source that can pull data from an HTTP endpoint that outputs JSON.
   */
  HTTP
}
