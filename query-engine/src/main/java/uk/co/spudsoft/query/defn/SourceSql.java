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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.time.Duration;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.sources.sql.SourceSqlStreamingInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceSql.Builder.class)
public class SourceSql implements Source {

  @Override
  public SourceInstance createInstance(Vertx vertx, Context context, SharedMap sharedMap, String defaultName) {
    return new SourceSqlStreamingInstance(vertx, context, sharedMap, this, defaultName);
  }

  private final SourceType type;
  private final String name;
  private final String endpoint;
  private final String endpointTemplate;
  private final String query;
  private final int streamingFetchSize;
  
  private final Integer maxPoolSize;
  private final Integer maxPoolWaitQueueSize;
  private final Duration idleTimeout;
  private final Duration connectionTimeout;
  private final Boolean replaceDoubleQuotes;
  
  @Override
  public void validate() {
    validateType(SourceType.SQL, type);
    if (Strings.isNullOrEmpty(endpoint) && Strings.isNullOrEmpty(endpointTemplate)) {
      throw new IllegalArgumentException("Neither endpoint nor endpointTemplate specified in SQL source");
    }
    if (Strings.isNullOrEmpty(query)) {
      throw new IllegalArgumentException("Query not specified in SQL source");
    }
    if (maxPoolSize != null) {
      if (maxPoolSize <= 0) {
        throw new IllegalArgumentException("MaxPoolSize must be at least 1");
      }
    }
    if (maxPoolWaitQueueSize != null) {
      if (maxPoolWaitQueueSize <= 0) {
        throw new IllegalArgumentException("MaxPoolWaitQueueSize must be at least 1");
      }
    }
    if (idleTimeout != null) {
      if (idleTimeout.getSeconds() < 0) {
        throw new IllegalArgumentException("IdleTimeout must be positive");
      }
    }
    if (connectionTimeout != null) {
      if (connectionTimeout.getSeconds() < 0) {
        throw new IllegalArgumentException("ConnectionTimeout must be positive");
      }
    }
  }
  
  @Override
  public SourceType getType() {
    return type;
  }

  @Override
  public String getName() {
    return name;
  }
  
  @Schema(description = """
                        <P>The name of the endpoint that provides the data for the Source.</P>
                        <P>
                        The endpoint represents with the HTTP endpoint or the SQL database that contains the actual data.
                        </P>
                        <P>
                        The endpoint must be specified as either a straight name (this field) or as a template value (endpointEmplate).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 200          
  )
  public String getEndpoint() {
    return endpoint;
  }

  @Schema(description = """
                        <P>A templated version of the name of the endpoint that provides the data for the Source.</P>
                        <P>
                        The endpoint represents with the HTTP endpoint or the SQL database that contains the actual data.
                        </P>
                        <P>
                        The endpoint must be specified as either a template value (this field) or as a straight name (endpoint).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 200          
  )
  public String getEndpointTemplate() {
    return endpointTemplate;
  }

  @Schema(description = """
                        <P>The query to run against the Endpoint.</P>
                        <P>
                        A SQL statement.
                        </P>
                        """
  )
  public String getQuery() {
    return query;
  }
  
  @Schema(description = """
                        <P>The number of rows to get from the Source at a time.</P>
                        <P>
                        A larger streaming fetch size will slow the initial data, but may be quicker overall (at the cost of more memory).
                        Experiment with values in the range 10-1000.
                        </P>
                        """
  )
  public int getStreamingFetchSize() {
    return streamingFetchSize;
  }    

  
  @Schema(description = """
                        <P>The maxmimum number of connections to open to the Endpoint.</P>
                        <P>
                        If there are likely to be multiple concurrent pipelines running to the same Endpoint it can be beneficial to set this to a small number, otherwise leave it at the default.
                        </P>
                        """
  )
  public Integer getMaxPoolSize() {
    return maxPoolSize;
  }

  @Schema(description = """
                        <P>The maxmimum number of connections have queued up for the Endpoint.</P>
                        <P>
                        This is unlikely to be useful.
                        </P>
                        """
  )
  public Integer getMaxPoolWaitQueueSize() {
    return maxPoolWaitQueueSize;
  }

  @Schema(description = """
                        <P>If set to true all double quotes in the query will be replaced with the identifier quoting character for the target.</P>
                        <P>
                        If the native quoting character is already a double quote no replacement will take place.
                        </P>
                        <P>
                        This enables queries for all database platforms to be defined using double quotes for identifiers, but it is a straight replacement
                        so if the query needs to contain a double quote that is not quoting an identifier then this must be set to false.
                        </P>
                        <P>
                        This is only useful when it is not known what flavour of database is being queried, which should be rare.
                        </P>
                        """
  )
  public Boolean getReplaceDoubleQuotes() {
    return replaceDoubleQuotes;
  }

  @Schema(
          description = """
                        <P>The idle timeout for the connection pool that will be created.</P>
                        <P>
                        After this time has passed the connection will be closed and a new one will be opened by subequent pipelines.
                        </P>
                        <P>
                        The value is an ISO8601 period string:  - the ASCII letter "P" in upper or lower case followed by four sections, each consisting of a number and a suffix.
                        The sections have suffixes in ASCII of "D", "H", "M" and "S" for days, hours, minutes and seconds, accepted in upper or lower case.
                        The suffixes must occur in order.
                        The ASCII letter "T" must occur before the first occurrence, if any, of an hour, minute or second section.
                        At least one of the four sections must be present, and if "T" is present there must be at least one section after the "T".
                        The number part of each section must consist of one or more ASCII digits.
                        The number of days, hours and minutes must parse to an long.
                        The number of seconds must parse to an long with optional fraction.
                        The decimal point may be either a dot or a comma.
                        The fractional part may have from zero to 9 digits.
                        </P>
                        <P>
                        The ISO8601 period format permits negative values, but they make no sense for timeouts and will cause an error.
                        </P>
                        """
          , implementation = String.class
          , maxLength = 100
  )
  @JsonFormat(shape = Shape.STRING)
  public Duration getIdleTimeout() {
    return idleTimeout;
  }

  @Schema(
          description = """
                        <P>The idle timeout for the connection pool that will be created.</P>
                        <P>
                        After this time has passed the connection will be closed and a new one will be opened by subequent pipelines.
                        </P>
                        <P>
                        The value is an ISO8601 period string:  - the ASCII letter "P" in upper or lower case followed by four sections, each consisting of a number and a suffix.
                        The sections have suffixes in ASCII of "D", "H", "M" and "S" for days, hours, minutes and seconds, accepted in upper or lower case.
                        The suffixes must occur in order.
                        The ASCII letter "T" must occur before the first occurrence, if any, of an hour, minute or second section.
                        At least one of the four sections must be present, and if "T" is present there must be at least one section after the "T".
                        The number part of each section must consist of one or more ASCII digits.
                        The number of days, hours and minutes must parse to an long.
                        The number of seconds must parse to an long with optional fraction.
                        The decimal point may be either a dot or a comma.
                        The fractional part may have from zero to 9 digits.
                        </P>
                        <P>
                        The ISO8601 period format permits negative values, but they make no sense for timeouts and will cause an error.
                        </P>
                        """
          , implementation = String.class
          , maxLength = 100
  )
  @JsonFormat(shape = Shape.STRING)
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.SQL;
    private String name;
    private String endpoint;
    private String endpointTemplate;
    private String query;
    private int streamingFetchSize = 1000;
    private Integer maxPoolSize;
    private Integer maxPoolWaitQueueSize;
    private Duration idleTimeout;
    private Duration connectionTimeout;
    private Boolean replaceDoubleQuotes;

    private Builder() {
    }

    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    public Builder endpoint(final String value) {
      this.endpoint = value;
      return this;
    }

    public Builder endpointTemplate(final String value) {
      this.endpointTemplate = value;
      return this;
    }

    public Builder query(final String value) {
      this.query = value;
      return this;
    }

    public Builder streamingFetchSize(final int value) {
      this.streamingFetchSize = value;
      return this;
    }

    public Builder maxPoolSize(final Integer value) {
      this.maxPoolSize = value;
      return this;
    }

    public Builder maxPoolWaitQueueSize(final Integer value) {
      this.maxPoolWaitQueueSize = value;
      return this;
    }

    public Builder idleTimeout(final Duration value) {
      this.idleTimeout = value;
      return this;
    }

    public Builder connectionTimeout(final Duration value) {
      this.connectionTimeout = value;
      return this;
    }

    public Builder replaceDoubleQuotes(final Boolean value) {
      this.replaceDoubleQuotes = value;
      return this;
    }

    public SourceSql build() {
      return new SourceSql(type, name, endpoint, endpointTemplate, query
          , streamingFetchSize
          , maxPoolSize, maxPoolWaitQueueSize, idleTimeout, connectionTimeout
          , replaceDoubleQuotes
      );
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public SourceSql(final SourceType type
          , final String name
          , final String endpoint
          , final String endpointTemplate
          , final String query
          , final int streamingFetchSize
          , final Integer maxPoolSize
          , final Integer maxPoolWaitQueueSize
          , final Duration idleTimeout
          , final Duration connectionTimeout
          , final Boolean replaceDoubleQuotes
  ) {
    validateType(SourceType.SQL, type);
    this.type = type;
    this.name = name;
    this.endpoint = endpoint;
    this.endpointTemplate = endpointTemplate;
    this.query = query;
    this.streamingFetchSize = streamingFetchSize;
    this.maxPoolSize = maxPoolSize;
    this.maxPoolWaitQueueSize = maxPoolWaitQueueSize;
    this.idleTimeout = idleTimeout;
    this.connectionTimeout = connectionTimeout;
    this.replaceDoubleQuotes = replaceDoubleQuotes;
  }

  
}
