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
package uk.co.spudsoft.query.defn;

/**
 * The type of ArgumentGroup.
 * These values are specific to Form.io rendering of arguments.
 * @author njt
 */
public enum ArgumentGroupType {
  
  /**
   * Use a Form.io FieldSet.
   * Just a group of fields with a label.
   */
  FIELD_SET
  
  ,
  /**
   * Use a Form.io Panel.
   * Panels have larger headings than field sets.
   */
  PANEL
  ,
  
  /**
   * Use a Form.io Panel with the collapsible option set.
   * Panels have larger headings than field sets, collapsible panels can be shrunk by the user.
   */
  COLLAPSIBLE_PANEL
  
}
