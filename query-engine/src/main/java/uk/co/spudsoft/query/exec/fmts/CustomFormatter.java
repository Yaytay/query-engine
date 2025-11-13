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

import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Interface to encourage common API for custom formatters.
 * 
 * @author jtalbut
 */
public interface CustomFormatter {
 
  /**
   * Format an object according to the configuration of this CustomFormatter.

   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param value The value to be formatted.
   * @return A string value that can be output.
   */
  String format(PipelineContext pipelineContext, Object value);
  
}
