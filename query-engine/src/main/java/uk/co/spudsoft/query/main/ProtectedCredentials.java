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
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.exec.dynamic.JexlEvaluator;

/**
 * Configuration of {@link Credentials} that also supports a {@link uk.co.spudsoft.query.defn.Condition}.
 * <p>
 * If a request does not meeting the Condition then it may not be used in the current pipeline.
 * @author jtalbut
 */
public class ProtectedCredentials extends Credentials {
  
  private Condition condition;

  /**
   * Constructor.
   */
  public ProtectedCredentials() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param username The username.
   * @param password The password.
   * @param condition An optional condition upon the credentials - unless this condition is met the credentials will not be usable by the current pipeline.
   */
  public ProtectedCredentials(String username, String password, Condition condition) {
    super(username, password);
    this.condition = condition;
  }

  /**
   * An optional condition upon the credentials - unless this condition is met the credentials will not be usable by the current pipeline.
   * @return optional condition upon the credentials 
   */
  public Condition getCondition() {
    return condition;
  }

  /**
   * An optional condition upon the credentials - unless this condition is met the credentials will not be usable by the current pipeline.
   * @param condition optional condition upon the credentials 
   */
  public void setCondition(Condition condition) {
    this.condition = condition;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (!Strings.isNullOrEmpty(getUsername())) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append("\"username\":\"").append(getUsername()).append('"');
    }
    if (!JexlEvaluator.isNullOrBlank(condition)) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append("\"condition\":\"").append(condition.getExpression()).append('"');
    }
    sb.insert(0, "{");
    sb.append("}");
    return sb.toString();
  }
  
}
