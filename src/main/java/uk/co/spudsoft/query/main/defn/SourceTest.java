/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.SourceInstance;
import uk.co.spudsoft.query.main.exec.sources.test.TestSource;

/**
 *
 * @author jtalbut
 */
public class SourceTest extends Source {

  private final int rowCount;

  public int getRowCount() {
    return rowCount;
  }

  @Override
  public SourceInstance<? extends Source> createInstance(Vertx vertx, Context context) {
    return new TestSource(context, this);
  }
  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.TEST;
    private int rowCount;

    private Builder() {
    }

    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    public Builder rowCount(final int value) {
      this.rowCount = value;
      return this;
    }

    public SourceTest build() {
      return new SourceTest(type, rowCount);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceTest(final SourceType type, final int rowCount) {
    super(type);
    this.rowCount = rowCount;
  }
    
}
