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
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;

/**
 *
 * @author jtalbut
 */
public class ProtectedCredentials extends Credentials {
  
  private Condition condition;

  public ProtectedCredentials() {
    super();
  }

  public ProtectedCredentials(String username, String password, Condition condition) {
    super(username, password);
    this.condition = condition;
  }
  
  public Condition getCondition() {
    return condition;
  }

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
    if (!ConditionInstance.isNullOrBlank(condition)) {
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
