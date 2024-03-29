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
package uk.co.spudsoft.query.main;

/**
 * Specify the type of sampling to use for traces.
 * @author jtalbut
 */
public enum TracingSampler {

  /**
   * Include every trace in the data sent to the tracing server.
   */
  alwaysOn
  , 
  /**
   * Don't include any traces in the data sent to the tracing server.
   */
  alwaysOff
  , 
  /**
   * If the trace has already been selected for reporting include it here.
   * If this is the first trace use the rootSampler.
   */
  parent
  , 
  /**
   * Include a percentage of the trace in the sample.
   */
  ratio
  
}
