/*
 * Copyright (C) 2026 jtalbut
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

import uk.co.spudsoft.query.defn.Condition;

/**
 * Conditions for defining operators.
 * @author jtalbut
 */
public class Operators {
  
  /**
   * The condition defining global operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by global operators.
   * 
   * A global operator  can access the request history, complete with logs and source details, of any user.
   */
  private Condition global;
  
  /**
   * The condition defining client operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by client operators.
   * 
   * A client operator can access the request history of any user with the same Issuer as their request.
   * Client operators cannot access logs or source details.
   */
  private Condition client;

  /**
   * Constructor.
   */
  public Operators() {
  }

  /**
   * Get the condition defining global operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by global operators.
   * 
   * A global operator  can access the request history, complete with logs and source details, of any user.
   * @return the condition defining global operators.
   */
  public Condition getGlobal() {
    return global;
  }

  /**
   * Set the condition defining global operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by global operators.
   * 
   * A global operator  can access the request history, complete with logs and source details, of any user.
   * @param global the condition defining global operators.
   */
  public void setGlobal(Condition global) {
    this.global = global;
  }

  /**
   * Get the condition defining client operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by client operators.
   * 
   * A client operator can access the request history of any user with the same Issuer as their request.
   * Client operators cannot access logs or source details.
   * 
   * @return the condition defining client operators.
   */
  public Condition getClient() {
    return client;
  }

  /**
   * Set the condition defining client operators.
   * 
   * If this condition is set then all requests meeting the condition are considered to be by client operators.
   * 
   * A client operator can access the request history of any user with the same Issuer as their request.
   * Client operators cannot access logs or source details.
   * 
   * @param client the condition defining client operators.
   */
  public void setClient(Condition client) {
    this.client = client;
  }

  /**
   * Validate the provided parameters.
   *
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  public void validate() throws IllegalArgumentException {
    if  (global != null) {
      global.validate();
    }
    if (client != null) {
      client.validate();
    }
  }
  
}
