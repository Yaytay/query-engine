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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  /**
   * Returns the first non-null argument from the provided list of arguments.
   * If all arguments are null or if no arguments are provided, this method returns null.
   *
   * @param args The variable number of arguments to evaluate. Each argument can be of any type.
   * @return The first non-null argument if found; otherwise, null if all arguments are null or none are provided.
   */
  public Object coalesce(Object... args) {
    for (Object arg : args) {
      if (arg != null) {
        return arg;
      }
    }
    return null;
  }

  /**
   * Finds the first string in the provided list that starts with the specified prefix.
   * If such a string is found and the removePrefix flag is set to true, the prefix
   * is removed from the found string before returning it. If no matching string is found,
   * this method returns null.
   *
   * @param strings The list of strings to search through. Can contain null elements.
   * @param prefix The prefix to look for in the strings from the list.
   * @param removePrefix A flag indicating whether to remove the prefix from the matching string.
   * @return The first string that matches the specified prefix, with or without the
   *         prefix removed based on the removePrefix flag, or null if no such string is found.
   */
  public String firstMatchingStringWithPrefix(List<String> strings, String prefix, boolean removePrefix) {
    if (strings != null) {
      for (String string : strings) {
        if (string != null && string.startsWith(prefix)) {
          if (removePrefix) {
            return string.substring(prefix.length());
          } else {
            return string;
          }
        }
      }
    }
    return null;
  }
  /**
   * Finds the first string in the provided list that starts with the specified prefix.
   * If such a string is found and the removePrefix flag is set to true, the prefix
   * is removed from the found string before returning it. If no matching string is found,
   * this method returns null.
   *
   * @param strings The list of strings to search through. Can contain null elements.
   * @param prefix The prefix to look for in the strings from the list.
   * @param removePrefix A flag indicating whether to remove the prefix from the matching string.
   * @return The first string that matches the specified prefix, with or without the
   *         prefix removed based on the removePrefix flag, or null if no such string is found.
   */
  public List<String> allStringsWithPrefix(List<String> strings, String prefix, boolean removePrefix) {
    List<String> result = new ArrayList<>();
    if (strings != null) {
      for (String string : strings) {
        if (string != null && string.startsWith(prefix)) {
          if (removePrefix) {
            result.add(string.substring(prefix.length()));
          } else {
            result.add(string);
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns the current date and time at the UTC time zone.
   *
   * @return The current date and time as a {@link LocalDateTime} in the UTC time zone.
   */
  public LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
  
  /**
   * Return an ArrayList of the passed in values.
   * 
   * JEXL can have some strange ways of handling arrays and lists, this method can be used to guarantee the type of a series.
   * 
   * The returned value is a copy of the list and changes made to either are independent of the other.
   * 
   * @param items a number of strings
   * @return an ArrayList containing the items
   */
  public List<String> list(String... items) {
    return new ArrayList<>(Arrays.asList(items));
  }

}
