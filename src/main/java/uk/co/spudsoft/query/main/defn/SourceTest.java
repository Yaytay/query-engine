/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;
import uk.co.spudsoft.query.main.exec.sources.test.TestSource;
import uk.co.spudsoft.query.main.exec.sources.test.TestSourceFactory;

/**
 *
 * @author jtalbut
 */
public class SourceTest extends Source {

  private static final TestSourceFactory FACTORY = new TestSourceFactory();
  
  private final int rowCount;

  public int getRowCount() {
    return rowCount;
  }
  
  @Override
  public SourceInstanceFactory<SourceTest, TestSource> getFactory() {
    return FACTORY;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type;
    private String endpoint;
    private String query;
    private int rowCount;

    private Builder() {
    }

    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    public Builder endpoint(final String value) {
      this.endpoint = value;
      return this;
    }

    public Builder query(final String value) {
      this.query = value;
      return this;
    }

    public Builder rowCount(final int value) {
      this.rowCount = value;
      return this;
    }

    public SourceTest build() {
      return new SourceTest(type, endpoint, query, rowCount);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceTest(final SourceType type, final String endpoint, final String query, final int rowCount) {
    super(type, endpoint, query);
    this.rowCount = rowCount;
  }
    
}
