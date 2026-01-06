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
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Instance class representing the {@link Condition}s from a (@link Operators} configuration.
 * 
 * @author jtalbut
 */
public class OperatorsInstance {

  private final ConditionInstance globalOperator;
  private final ConditionInstance clientOperator;

  /**
   * Constructor.
   * 
   * @param operatorsConfiguration The configured conditions that must be met for the current request to be considered to be from an operator, may be null.
   */
  public OperatorsInstance(Operators operatorsConfiguration) {
    if (operatorsConfiguration == null) {
      this.globalOperator = null;
      this.clientOperator = null;
    } else {
      this.globalOperator = operatorsConfiguration.getGlobal() == null ? null : operatorsConfiguration.getGlobal().createInstance();
      this.clientOperator = operatorsConfiguration.getClient() == null ? null : operatorsConfiguration.getClient().createInstance();
    }
  }
  
  /**
   * Details of  whether the current token represents an Operator, according to the rules from the {@link Operators} configuration.
   * 
   * Note that only global operators have access to information that might leak configuration information.
   * 
   * @param global If true the user is a global operator and has full access to systems data.
   * @param client If true the user is a client operator and has access to data relating to the same issuer.
   */
  public record Flags(boolean global, boolean client) {
    @Override
    public String toString() {
      if (global) {
        return "global operator";
      } else if (client) {
        return "client operator";
      } else {
        return "normal user";
      }
    }
  }

  /**
   * Evaluate any conditions in the {@link Operators} configuration against the current {@link RequestContext}.
   * 
   * @param ctx The context of the current request.
   * @return a newly constructed Flags object.
   */
  public Flags evaluate(RequestContext ctx) {
    return new Flags(
            evaluateCondition(globalOperator, ctx)
            , evaluateCondition(clientOperator, ctx)
    );
  }

  private static boolean evaluateCondition(ConditionInstance c, RequestContext ctx) {
    return c != null && c.evaluate(ctx, null, null);
  }
  
}
