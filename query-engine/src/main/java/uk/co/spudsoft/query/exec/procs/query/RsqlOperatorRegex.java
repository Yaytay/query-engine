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
package uk.co.spudsoft.query.exec.procs.query;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * RSQL operator for performing string matches.
 * 
 * This is expected to only be used with string fields, if it is used with non-strings they will be converted
 * to strings using the standard {@link Object#toString()} method.
 * 
 * @author jtalbut
 */
public class RsqlOperatorRegex extends RsqlEvaluator.RsqlOperatorString {

  /**
   * Default constructor.
   */
  public RsqlOperatorRegex() {
  }
  
  @Override
  boolean compare(String rowValue, List<String> args) {
    
    if (args.size() != 1) {
      throw new IllegalArgumentException(Integer.toString(args.size()) + " arguments provided to =~ operator");
    }
    String argument = args.get(0);
    if (rowValue == null || argument == null) {
      return false;
    }
    Pattern pattern;
    try {
      pattern = Pattern.compile(argument);
    } catch (PatternSyntaxException ex) {
      throw new IllegalArgumentException("Invalid argument passed to =~ operator", ex);
    }
    
    return pattern.matcher(rowValue).matches();

  }

  
}
