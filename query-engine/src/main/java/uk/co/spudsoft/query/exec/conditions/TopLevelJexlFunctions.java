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
package uk.co.spudsoft.query.exec.conditions;

/**
 * A utility class to provide custom methods for logical operations in JEXL expressions.
 * @author jtalbut
 */
public class TopLevelJexlFunctions {

  /**
   * Do nothing constructor.
   */
  public TopLevelJexlFunctions() {
  }

  /**
   * Evaluates a logical AND operation on the provided Boolean arguments.
   * Returns true if all arguments are non-null and true; otherwise, returns false.
   *
   * @param args The Boolean arguments to evaluate. Each argument can be true, false, or null.
   * @return true if all arguments are non-null and true, false otherwise.
   */
  public boolean andFn(Boolean... args) {
    for (Boolean arg : args) {
      if (arg == null || !arg) {
        return false;
      }
    }
    return true;
  }

  /**
   * Evaluates a logical OR operation on the provided Boolean arguments.
   * Returns true if at least one argument is non-null and true; otherwise, returns false.
   *
   * @param args The Boolean arguments to evaluate. Each argument can be true, false, or null.
   * @return true if at least one argument is non-null and true, false otherwise.
   */
  public boolean orFn(Boolean... args) {
    for (Boolean arg : args) {
      if (arg != null && arg) {
        return true;
      }
    }
    return false;
  }

}
