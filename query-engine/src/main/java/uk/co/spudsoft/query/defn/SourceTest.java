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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.sources.test.SourceTestInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceTest.Builder.class)
public class SourceTest implements Source {

  private final SourceType type;
  private final int rowCount;
  private final String name;
  private final int delayMs;

  @Override
  public void validate() {
    validateType(SourceType.TEST, type);
  }
  
  @Override
  public SourceType getType() {
    return type;
  }

  public int getRowCount() {
    return rowCount;
  }

  @Override
  public String getName() {
    return name;
  }  

  public int getDelayMs() {
    return delayMs;
  }
  
  @Override
  public SourceInstance createInstance(Vertx vertx, Context context, SharedMap sharedMap, String defaultName) {
    return new SourceTestInstance(context, this, defaultName);
  }
  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.TEST;
    private int rowCount;
    private String name;
    private int delayMs;

    private Builder() {
    }

    public SourceTest build() {
      return new SourceTest(type, rowCount, name, delayMs);
    }

    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    public Builder rowCount(final int value) {
      this.rowCount = value;
      return this;
    }

    public Builder name(final String value) {
      this.name = value;
      return this;
    }
    
    public Builder delayMs(final int value) {
      this.delayMs = value;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceTest(final SourceType type, final int rowCount, final String name, int delayMs) {
    validateType(SourceType.TEST, type);
    this.type = type;
    this.rowCount = rowCount;
    this.name = name;
    this.delayMs = delayMs;
  }
    
}
