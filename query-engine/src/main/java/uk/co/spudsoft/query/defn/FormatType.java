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

/**
 * The generic format of the output.
 *
 * @author jtalbut
 */
public enum FormatType {
  /**
   * Output the data as Json.
   */
  JSON
  ,
  /**
   * Output the data as XML.
   */
  XML
  ,
  /**
   * Output the data as an XLSX file.
   */
  XLSX
  ,
  /**
   * Output the data as a delimited text file.
   */
  Delimited
  ,
  /**
   * Output the data as an HTML table.
   */
  HTML
  ,
  /**
   * Output the data as an Atom feed.
   */
  Atom
  ,
  /**
   * Output the data as an RSS feed.
   * Atom feeds should be used in preference when possible.
   */
  RSS
}
