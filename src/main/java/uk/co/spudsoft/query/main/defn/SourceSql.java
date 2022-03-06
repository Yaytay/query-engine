/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import uk.co.spudsoft.query.main.exec.sources.sql.BlockingSqlQuerySourceFactory;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;
import uk.co.spudsoft.query.main.exec.sources.sql.BlockingSqlQuerySource;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceSql.Builder.class)
public class SourceSql extends Source {

  private static final BlockingSqlQuerySourceFactory FACTORY = new BlockingSqlQuerySourceFactory();

  @Override
  public SourceInstanceFactory<SourceSql, BlockingSqlQuerySource> getFactory() {
    return FACTORY;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type;
    private String endpoint;
    private String query;

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

    public SourceSql build() {
      return new SourceSql(type, endpoint, query);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceSql(final SourceType type, final String endpoint, final String query) {
    super(type, endpoint, query);
  }

}
