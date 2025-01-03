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
 * The SourceNameTracker is used to allow {@link SourceInstance} objects to record their ID in the context for logging.
 * 
 * @author jtalbut
 */
public interface SourceNameTracker {
  
  /**
   * Add the Source name to the local context data map for use in logging.
   */
  void addNameToContextLocalData();
  
}
