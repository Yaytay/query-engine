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
package uk.co.spudsoft.query.defn;

import uk.co.spudsoft.query.defn.Condition;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class ConditionTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ConditionTest.class);
  
  @Test
  public void testGetExpression() {
    Condition cond = new Condition("bob");
    assertEquals("bob", cond.getExpression());
  }

  @Test
  public void testCreateInstance() {
    Condition cond = new Condition("true");
    ConditionInstance instance = cond.createInstance();
  }
  
  @JsonDeserialize(builder = Owner.Builder.class)
  private static class Owner {
    private final Condition condition;

    public Condition getCondition() {
      return condition;
    }

    @Override
    public String toString() {
      return "Owner{" + "condition=" + condition + '}';
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    public static class Builder {

      private Condition condition;

      private Builder() {
      }

      public Builder condition(final Condition value) {
        this.condition = value;
        return this;
      }

      public Owner build() {
        return new uk.co.spudsoft.query.defn.ConditionTest.Owner(condition);
      }
    }

    public static Owner.Builder builder() {
      return new Owner.Builder();
    }

    private Owner(final Condition condition) {
      this.condition = condition;
    }
  }
  
  @Test
  public void testJson() {
    
    Condition cond = new Condition("1 == 3-2");
    Owner owner = Owner.builder().condition(cond).build();
    
    String json = Json.encode(owner);
    logger.debug("Json: {}", json);
    assertEquals("{\"condition\":\"1 == 3-2\"}", json);
    
    Owner owner2 = Json.decodeValue(json, Owner.class);
    logger.debug("Owners: {} & {}", owner, owner2);
    assertEquals(owner.getCondition().getExpression(), owner2.getCondition().getExpression());
    
  }
  
}
