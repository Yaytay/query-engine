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

package uk.co.spudsoft.query.exec.dynamic;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Accessor methods for creating standard objects that may be useful in a StringTemplate.
 * @author jtalbut
 */
public class TopLevelStringTemplateObjects {

  /**
   * Constructor.
   */
  public TopLevelStringTemplateObjects() {
  }
  
  /**
   * Get the current time (UTC).
   * @return the current time (UTC).
   */
  public LocalDateTime getNow() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
