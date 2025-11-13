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
package uk.co.spudsoft.query.exec.fmts;

import com.google.common.base.Strings;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * A helper class to work with DecimalFormat.
 *
 * @author jtalbut
 */
public class CustomDecimalFormatter implements CustomFormatter {

  private static final Logger logger = LoggerFactory.getLogger(CustomDecimalFormatter.class);
  
  private final DecimalFormat formatter;
  private final boolean mustBeEncodedAsString;

  /**
   * Constructor.
   *
   * Note that DecimalFormat is quite lenient with its patterns.
   * For example a pattern for "invalid" will result in an output of either "invalid" or "-invalid" for any input.
   * Misuse of real format characters is more likely to cause an exception.
   * 
   * @param format a DecimalFormat pattern.
   */
  public CustomDecimalFormatter(String format) {

    if (Strings.isNullOrEmpty(format)) {
      formatter = null;
      mustBeEncodedAsString = false;
    } else {
      formatter = new DecimalFormat(format);
      mustBeEncodedAsString = mustBeEncodedAsString(format);
    }
  }

  
  
  private boolean mustBeEncodedAsString(String format) {

    int dashCount = 0;
    int eCount = 0;
    int semicolonCount = 0;
    for (int i = 0; i < format.length();) {
      int codePoint = format.codePointAt(i);
      switch (codePoint) {
        case (int) '0':
        case (int) '#':
        case (int) '.':
          break ;
        case (int) '-':
          if (++dashCount > 1) {
            return true;
          } 
          break;
        case (int) ';':
          if (++semicolonCount > 1) {
            return true;
          } 
          break;
        case (int) 'E':
          if (++eCount > 1) {
            return true;
          } 
          break;
        default:
          return true;
      }
      i += Character.charCount(codePoint);
    }
    
    return false;
  }

  /**
   * Return true if the output from the format pattern might contain things other than numbers.
   *
   * @return true if the output from the format pattern might contain things other than numbers.
   */
  public boolean mustBeEncodedAsString() {
    return mustBeEncodedAsString;
  }

  /**
   * Format the date/time value according to the configured formatter.
   *
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param value the date/time value to be formatted.
   * @return The formatted value.
   */
  @Override
  public String format(PipelineContext pipelineContext, Object value) {
    if (value == null) {
      return null;
    } else {
      if (value instanceof Number n) {
        if (formatter == null) {
          return n.toString();
        } else {
          return formatter.format(n);
        }
      } else {
        Log.decorate(logger.atWarn(), pipelineContext).log("Value {} of type {} passed to CustomDecimalFormatter", value, value.getClass());
        return value.toString();
      }
    }
  }

}
