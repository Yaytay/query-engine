/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import uk.co.spudsoft.query.main.exec.sources.http.HttpQuerySourceFactory;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;
import uk.co.spudsoft.query.main.exec.sources.http.HttpQuerySource;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceHttp.Builder.class)
public class SourceHttp extends Source {

  private static final HttpQuerySourceFactory FACTORY = new HttpQuerySourceFactory();

  @Override
  public SourceInstanceFactory<SourceHttp, HttpQuerySource> getFactory() {
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

    public SourceHttp build() {
      return new SourceHttp(type, endpoint, query);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceHttp(final SourceType type, final String endpoint, final String query) {
    super(type, endpoint, query);
  }
    
}
