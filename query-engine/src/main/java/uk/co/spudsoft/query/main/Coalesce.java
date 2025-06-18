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
package uk.co.spudsoft.query.main;

import com.google.common.base.Strings;

/**
 * Helper class providing implementations of coalesce functions.
 * @author jtalbut
 */
public class Coalesce {

  private Coalesce() {
  }
  
  /**
   * Return the first argument that is not null or empty.
   * If the first argument is null or empty the second is returned regardless of its value.
   * @param one The first argument.
   * @param two The second argument.
   * @return the first argument that is not null or empty.
   */
  public static String coalesce(String one, String two) {
    if (!Strings.isNullOrEmpty(one)) {
      return one;
    }
    return two;
  }
  
  /**
   * Return the first argument that is not null or empty.
   * If the first and second arguments are null or empty the third is returned regardless of its value.
   * @param one The first argument.
   * @param two The second argument.
   * @param three The third argument.
   * @return the first argument that is not null or empty.
   */
  public static String coalesce(String one, String two, String three) {
    if (!Strings.isNullOrEmpty(one)) {
      return one;
    }
    if (!Strings.isNullOrEmpty(two)) {
      return two;
    }
    return three;
  }
  
}
