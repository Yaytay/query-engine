/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PoolOptions;
import uk.co.spudsoft.query.main.exec.SourceInstance;
import uk.co.spudsoft.query.main.exec.sources.sql.SourceSqlStreamingInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceSql.Builder.class)
public class SourceSql implements Source {

  @Override
  public SourceInstance createInstance(Vertx vertx, Context context) {
    return new SourceSqlStreamingInstance(vertx, context, this);
  }

  private final SourceType type;
  private final String endpoint;
  private final String query;
  private final PoolOptions poolOptions;
  private final int streamingFetchSize;

  public SourceType getType() {
    return type;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getQuery() {
    return query;
  }
  
  public PoolOptions getPoolOptions() {
    return poolOptions;
  }

  public int getStreamingFetchSize() {
    return streamingFetchSize;
  }    

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.SQL;
    private String endpoint;
    private String query;
    private PoolOptions poolOptions;
    private int streamingFetchSize = 1000;

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

    public Builder poolOptions(final PoolOptions value) {
      this.poolOptions = value;
      return this;
    }

    public Builder streamingFetchSize(final int value) {
      this.streamingFetchSize = value;
      return this;
    }

    public SourceSql build() {
      return new SourceSql(type, endpoint, query, poolOptions, streamingFetchSize);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceSql(final SourceType type
          , final String endpoint
          , final String query
          , final PoolOptions poolOptions
          , final int streamingFetchSize
  ) {
    this.type = type;
    this.endpoint = endpoint;
    this.query = query;
    this.poolOptions = poolOptions;
    this.streamingFetchSize = streamingFetchSize;
  }

}
