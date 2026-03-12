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
   * @param delimiter Delimiter to put before each exception except the first.
   * @param stackDelimiter Delimiter to put before each stack trace entry.
   * @param stackCount Limit to the number of stack trace rows to output per exception
   * @return A string detailing the exception.
   */
  public static String convert(Throwable ex, String delimiter, String stackDelimiter, int stackCount) {
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
      result.append(current.getClass().getSimpleName());
      if (!Strings.isNullOrEmpty(current.getMessage())) {
        result.append(": ").append(current.getMessage());
      }
      appendStackDetails(stackTrace, stackDelimiter, stackCount, result);
    }
    return result.toString();   
  }  

  private static void appendStackDetails(StackTraceElement[] stackTrace, String stackDelimiter, int stackCount, StringBuilder result) {
    if (stackTrace != null && stackTrace.length > 0) {
      for (int i = 0; i < stackCount && i < stackTrace.length; ++i) {
        StackTraceElement ste = stackTrace[i];
        result.append(stackDelimiter).append(ste.getClassName());
        if (ste.getLineNumber() > 0) {
          result.append(":").append(ste.getLineNumber());
        }
      }
    }
  }
}
