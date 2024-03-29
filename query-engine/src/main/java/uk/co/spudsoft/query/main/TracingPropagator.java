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
 * The mechanism that will be used for propagating trace/span details between services.
 * 
 * All services in an environment must use the same propagation protocol.
 * 
 * @author jtalbut
 */
public enum TracingPropagator {

  /**
   * Generate headers for propagation following the W3C Trace Context recommendation.
   * See <a href="https://www.w3.org/TR/trace-context/">https://www.w3.org/TR/trace-context/</a> for more details.
   */
  w3c
  , 
  
  /**
   * Generate a single B3 header for propagating.
   * See <a href="https://github.com/openzipkin/b3-propagation/tree/master">https://github.com/openzipkin/b3-propagation/tree/master</a> for more details.
   */
  b3
  , 
  
  /**
   * Use multiple B3 headers for propagating.
   * See <a href="https://github.com/openzipkin/b3-propagation/tree/master">https://github.com/openzipkin/b3-propagation/tree/master</a> for more details.
   */
  b3multi
  
}
