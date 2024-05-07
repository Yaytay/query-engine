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
package uk.co.spudsoft.query.exec.filters;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing a string of space delimited values into a List.
 * 
 * @author jtalbut
 */
public class SpaceParser {
  
  private static final int SPACE = 0x20;
  
  /**
   * Convert a single string of space delimited items into a List of separate strings.
   * 
   * Any string of consecutive spaces will be converted into a string a one fewer spaces rather than being considered a delimiter.
   * It is not possible to have a resulting item that begins or ends with a space.
   * 
   * The function should be Unicode safe (as much as Java is).
   * 
   * Examples:
   * <ul>
   * <li>"a b" => ["a", "b"]
   * <li>"a  b" => ["a b"]
   * <li>"a   b" => ["a  b"]
   * </ul>
   * 
   * @param input
   * @return a List of separate strings.
   */
  public static List<String> parse(String input) {
    List<String> output = new ArrayList<>();
    if (!Strings.isNullOrEmpty(input)) {
      StringBuilder current = new StringBuilder();
      // inSpace[0] means it's seen at least one space, inSpace[1] means it's seen more than one space
      boolean[] inSpace = {false, false};
      input.codePoints()
              .forEach(cp -> {
                if (cp == SPACE) {
                  if (inSpace[0]) {
                    current.appendCodePoint(cp);
                    inSpace[1] = true;
                  } else {
                    inSpace[0] = true;
                  }
                } else {
                  if (inSpace[1]) {
                    inSpace[0] = false;
                    inSpace[1] = false;
                  } else if (inSpace[0]) {
                    inSpace[0] = false;
                    output.add(current.toString());
                    current.setLength(0);
                  }
                  current.appendCodePoint(cp);
                }
              });
      if (!current.isEmpty()) {
        output.add(current.toString());
      }
    }
    return output;
  }
  
}
