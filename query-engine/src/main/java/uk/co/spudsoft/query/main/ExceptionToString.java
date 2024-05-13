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
package uk.co.spudsoft.query.main;

import com.google.common.base.Strings;

/**
 * Helper class to convert an exception to a string in a similar manner to logback.
 * 
 * @author jtalbut
 */
public class ExceptionToString {
  
  private ExceptionToString() {
  }
  
  /**
   * Convert an exception to a string detailing the exception, it's stack trace and the chain of causes.
   * @param ex The {@link java.lang.Throwable} to process.
   * @param delimiter Delimiter to put between each exception.
   * @return A string detailing the exception.
   */
  public static String convert(Throwable ex, String delimiter) {
    if (ex == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    boolean started = false;
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (started) {
        result.append(delimiter);
      } else {
        started = true;
      }
      StackTraceElement[] stackTrace = current.getStackTrace();
      if (Strings.isNullOrEmpty(current.getMessage())) {
        result.append(current.getClass().getSimpleName());
        appendClassDetails(stackTrace, result);
      } else {
        result.append(current.getMessage())
                .append(" (from ")
                .append(current.getClass().getSimpleName());
        appendClassDetails(stackTrace, result);
        result.append(")");
      }
    }
    return result.toString();   
  }  

  private static void appendClassDetails(StackTraceElement[] stackTrace, StringBuilder result) {
    if (stackTrace != null && stackTrace.length > 0) {
      result.append("@").append(stackTrace[0].getClassName());
      if (stackTrace[0].getLineNumber() > 0) {
        result.append(":").append(stackTrace[0].getLineNumber());
      }
    }
  }
}
