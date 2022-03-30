/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.SourceInstance;
import uk.co.spudsoft.query.main.exec.sources.test.SourceTestInstance;

/**
 *
 * @author jtalbut
 */
public class SourceTest implements Source {

  private final SourceType type;
  private final int rowCount;
  private final String name;

  @Override
  public SourceType getType() {
    return type;
  }

  public int getRowCount() {
    return rowCount;
  }

  public String getName() {
    return name;
  }  

  @Override
  public SourceInstance createInstance(Vertx vertx, Context context) {
    return new SourceTestInstance(context, this);
  }
  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.TEST;
    private int rowCount;
    private String name;

    private Builder() {
    }

    public SourceTest build() {
      return new SourceTest(type, rowCount, name);
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
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceTest(final SourceType type, final int rowCount, final String name) {
    validateType(SourceType.TEST, type);
    this.type = type;
    this.rowCount = rowCount;
    this.name = name;
  }
    
}
