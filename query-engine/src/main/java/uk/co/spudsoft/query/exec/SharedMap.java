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
package uk.co.spudsoft.query.exec;

/**
 * A very simple Map interface to enable pooling between {@link SourceInstance} objects.
 * @author jtalbut
 */
public interface SharedMap {
  
  /**
   * Get a named object from the map.
   * @param name The name of the object in the map.
   * @return The value that was previously put into the map with the {@link #put(java.lang.String, java.lang.Object)} method.
   */
  Object get(String name);
  
  /**
   * Put a value into the map.
   * @param name The name of the object to put in the map.
   * @param value The object to put in the map.
   */
  void put(String name, Object value);
  
}
