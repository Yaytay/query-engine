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
 * The type of Processor.
 *
 * @author jtalbut
 */
@Schema(description = """
                      The type of Processor.
                      """)
public enum ProcessorType {

  /**
   * The {@link Processor} is a {@link ProcessorLimit} that restricts the number of rows output.
   */
  LIMIT, 
  /**
   * The {@link Processor} is a {@link ProcessorOffset} that skips a number of initial rows in the output.
   */
  OFFSET, 
  /**
   * The {@link Processor} is a {@link ProcessorMerge} that adds all the fields from a child query.
   */
  MERGE, 
  /**
   * The {@link Processor} is a {@link ProcessorGroupConcat} that concatenates fields from multiple rows in a child query.
   */
  GROUP_CONCAT, 
  /**
   * The {@link Processor} is a {@link ProcessorDynamicField} that create new fields from multiple rows in a child query.
   */
  DYNAMIC_FIELD, 
  /**
   * The {@link Processor} is a {@link ProcessorLookup} that creates new fields by looking up values in a map created from a child query.
   */
  LOOKUP, 
  /**
   * The {@link Processor} is a {@link ProcessorScript} that runs a script to either act as a predicate per row or to modify rows.
   */
  SCRIPT, 
  /**
   * The {@link Processor} is a {@link ProcessorExpression} that runs a JEXL expression to either act as a predicate per row or to set a field on the row.
   */
  EXPRESSION, 
  /**
   * The {@link Processor} is a {@link ProcessorQuery} that uses a FIQL expression to filter rows.
   */
  QUERY, 
  /**
   * The {@link Processor} is a {@link ProcessorMap} that either renames fields or drops them.
   */
  MAP, 
  /**
   * The {@link Processor} is a {@link ProcessorSort} that sorts the output rows.
   */
  SORT

}
