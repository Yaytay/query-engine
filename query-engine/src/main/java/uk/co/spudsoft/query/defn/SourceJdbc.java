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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.sources.jdbc.SourceJdbcInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Pipeline data source that gets data from a SQL database.
 * <P>
 * This is the standard source of data for pipelines.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceJdbc.Builder.class)
@Schema(description = """
                      Pipeline data source that gets data from a SQL database.
                      <P>
                      This is the standard source of data for pipelines.
                      """)
public final class SourceJdbc implements Source {

  @Override
  public SourceJdbcInstance createInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, SharedMap sharedMap) {
    return new SourceJdbcInstance(vertx, meterRegistry, auditor, pipelineContext, this);
  }

  /**
   * The type of Source being configured.
   */
  private final SourceType type;
  /**
   * Get the name of the Source, that will be used in logging.
   * This is optional, if it is not set a numeric (or delimited numeric) name will be allocated.
   */
  private final String name;
  private final String endpoint;
  private final String endpointTemplate;
  private final String query;
  private final String queryTemplate;
  private final int jdbcFetchSize;
  private final int processingBatchSize;
  
  private final Duration connectionTimeout;
  private final Boolean replaceDoubleQuotes;
  private final ImmutableList<ColumnType> columnTypeOverrides;
  private final ImmutableMap<String, DataType> columnTypeOverrideMap;
  
  
  @Override
  public void validate(PipelineContext pipelineContext) throws IllegalArgumentException {
    validateType(SourceType.JDBC, type);
    if (Strings.isNullOrEmpty(endpoint) && Strings.isNullOrEmpty(endpointTemplate)) {
      throw new IllegalArgumentException("Neither endpoint nor endpointTemplate specified in JDBC source");
    }
    if (Strings.isNullOrEmpty(query) && Strings.isNullOrEmpty(queryTemplate)) {
      throw new IllegalArgumentException("Neither query nor queryTemplate specified in JDBC source");
    }
    if (!Strings.isNullOrEmpty(query) && !Strings.isNullOrEmpty(queryTemplate)) {
      throw new IllegalArgumentException("Both query and queryTemplate specified in JDBC source");
    }
    if (connectionTimeout != null) {
      if (connectionTimeout.getSeconds() < 0) {
        throw new IllegalArgumentException("ConnectionTimeout must be positive");
      }
    }
    if (processingBatchSize <= 100) {
      throw new IllegalArgumentException("ProcessingBatchSize must be at least 100");
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
  
  /**
   * The name of the endpoint that provides the data for the Source.
   * <P>
   * The endpoint represents the SQL database that contains the actual data.
   * <P>
   * The endpoint must be specified as either a straight name (this field) or as a template value (endpointEmplate).
   * If both fields are provided it is an error.
   * 
   * @return the name of the endpoint that provides the data for the Source.
   */
  @Schema(description = """
                        <P>The name of the endpoint that provides the data for the Source.</P>
                        <P>
                        The endpoint represents the SQL database that contains the actual data.
                        </P>
                        <P>
                        The endpoint must be specified as either a straight name (this field) or as a template value (endpointEmplate).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 100
  )
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * A <a href="http://www.stringtemplate.org">String Template</a> version of the name of the endpoint that provides the data for the Source.
   * <P>
   * The endpoint represents the SQL database that contains the actual data.
   * <P>
   * The endpoint must be specified as either a template value (this field) or as a straight name (endpoint).
   * If both fields are provided it is an error.
   * 
   * @return a <a href="http://www.stringtemplate.org">String Template</a> version of the name of the endpoint that provides the data for the Source.
   */
  @Schema(description = """
                        <P>A <a target="_blank" href="http://www.stringtemplate.org">String Template</a> version of the name of the endpoint that provides the data for the Source.</P>
                        <P>
                        The endpoint represents the SQL database that contains the actual data.
                        </P>
                        <P>
                        The endpoint must be specified as either a template value (this field) or as a straight name (endpoint).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 1000000
  )
  public String getEndpointTemplate() {
    return endpointTemplate;
  }

  /**
   * The query to run against the Endpoint.
   * <P>
   * A SQL statement.
   * <p>
   * The query must be specified as either a plain SQL statement (this field) or as a template value (queryTemplate).
   * If both fields are provided it is an error.
   * 
   * @return the query to run against the Endpoint.
   */
  @Schema(description = """
                        <P>The query to run against the Endpoint.</P>
                        <P>
                        A SQL statement.
                        </P>
                        <P>
                        The query must be specified as either a plain SQL statement (this field) or as a template value (queryTemplate).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 1000000
  )
  public String getQuery() {
    return query;
  }
  
  /**
   * The query to run against the Endpoint, as a <a href="http://www.stringtemplate.org">String Template</a> that will be rendered first.
   * <P>
   * A StringTemplate that results in a SQL statement.
   * <p>
   * The query must be specified as either a templated value (this field) or as a plain SQL statement (query).
   * If both fields are provided it is an error.
   * @return the query to run against the Endpoint, as a <a href="http://www.stringtemplate.org">String Template</a> that will be rendered first.
   */
  @Schema(description = """
                        <P>The query to run against the Endpoint, as a <a target="_blank" href="http://www.stringtemplate.org">String Template</a> that will be rendered first.</P>
                        <P>
                        A StringTemplate that results in a SQL statement.
                        </P>
                        <p>
                        The query must be specified as either a templated value (this field) or as a plain SQL statement (query).
                        If both fields are provided it is an error.
                        </P>
                        """
          , maxLength = 1000000
  )
  public String getQueryTemplate() {
    return queryTemplate;
  }

  /**
   * The JDBC fetch size to use when retrieving data from the database.
   * <P>
   * This controls how many rows are fetched from the database at once by the JDBC driver.
   * A larger fetch size can improve performance by reducing the number of round trips to the database,
   * but uses more memory.
   * <P>
   * Typical values are in the range 100-5000.
   * 
   * @return the JDBC fetch size to use when retrieving data from the database.
   */
  @Schema(description = """
                        <P>The JDBC fetch size to use when retrieving data from the database.</P>
                        <P>
                        This should control how many rows are fetched from the database at once by the JDBC driver.
                        A larger fetch size can improve performance by reducing the number of round trips to the database,
                        but uses more memory.
                        </P>
                        <P>
                        In order to stream data reliably some JDBC drivers need specific values, i.e. for MySQL the value of -2147483648 is required for streaming.
                        If this value is set to any negative value the system will attempt to determine an appropriate value based on the URL, specifically choosing based on the start of the URL:
                        <UL>
                        <LI>jdbc:mysql:  -2147483648
                        <LI>jdbc:sqlserver:  1000
                        <LI>jdbc:postgres: 1000
                        <LI>otherwise: 1000
                        </UL>
                        This is intended to mean that a negative value does something appropriate for streaming.
                        </P>
                        <P>
                        Typical values are in the range 100-5000.
                        </P>
                        """
          , defaultValue = "-1"
  )
  public int getJdbcFetchSize() {
    return jdbcFetchSize;
  }

  /**
   * The number of rows to buffer for processing in the pipeline.
   * <P>
   * This controls how many rows are buffered in memory for processing by the pipeline.
   * A larger processing batch size can improve throughput but uses more memory.
   * <P>
   * Typical values are in the range 100-1000.
   * 
   * @return the number of rows to buffer for processing in the pipeline.
   */
  @Schema(description = """
                        <P>The number of rows to buffer for processing in the pipeline.</P>
                        <P>
                        This controls how many rows are buffered in memory for processing by the pipeline.
                        A larger processing batch size can improve throughput but uses more memory.
                        </P>
                        <P>
                        Typical values are in the range 10-1000.
                        </P>
                        """
          , minimum = "100"
          , defaultValue = "1000"
  )
  public int getProcessingBatchSize() {
    return processingBatchSize;
  }

  /**
   * If set to true all double quotes in the query will be replaced with the identifier quoting character for the target.
   * <P>
   * If the native quoting character is already a double quote no replacement will take place.
   * <P>
   * This enables queries for all database platforms to be defined using double quotes for identifiers, but it is a straight replacement
   * so if the query needs to contain a double quote that is not quoting an identifier then this must be set to false.
   * <P>
   * This is only useful when it is not known what flavour of database is being queried, which should be rare.
   * 
   * @return true if all double quotes in the query should be replaced with the identifier quoting character for the target.
   */
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

  /**
   * The connection timeout for the connections that will be created.
  * <P>
   * The value is an ISO8601 period string:  - the ASCII letter "P" in upper or lower case followed by four sections, each consisting of a number and a suffix.
   * The sections have suffixes in ASCII of "D", "H", "M" and "S" for days, hours, minutes and seconds, accepted in upper or lower case.
   * The suffixes must occur in order.
   * The ASCII letter "T" must occur before the first occurrence, if any, of an hour, minute or second section.
   * At least one of the four sections must be present, and if "T" is present there must be at least one section after the "T".
   * The number part of each section must consist of one or more ASCII digits.
   * The number of days, hours and minutes must parse to an long.
   * The number of seconds must parse to an long with optional fraction.
   * The decimal point may be either a dot or a comma.
   * The fractional part may have from zero to 9 digits.
   * <P>
   * The ISO8601 period format permits negative values, but they make no sense for timeouts and will cause an error.
   * 
   * @return the connection timeout for the connections that will be created.
   */
  @Schema(
          description = """
                        <P>The connection timeout for the connections that will be created.</P>
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
  )
  @JsonFormat(shape = Shape.STRING)
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Get the overrides for column types.
   * 
   * This is a map of column names (from the results for this query) to the Query Engine {@link DataType} that should be used in the
   * result stream.
   * 
   * This facility is rarely required, but can be useful when a data base does not provide adequate information for Query Engine to correctly identify the type of a field.
   * 
   * This is known to be useful for boolean fields with MySQL.
   * 
   * Setting a column to use a type that the result does not fit is going to cause problems (loss of data or errors) - so be sure you do this with care.
   * 
   * @return the overrides for column types.
   */
  @Schema(
          description = """
                        Get the overrides for column types.
                        <P>
                        This is a map of column names (from the results for this query) to the Query Engine {@link DataType} that should be used in the
                        result stream.
                        <P>
                        This facility is rarely required, but can be useful when a data base does not provide adequate information for Query Engine to correctly identify the type of a field.
                        <P>
                        This is known to be useful for boolean fields with MySQL.
                        <P>
                        Setting a column to use a type that the result does not fit is going to cause problems (loss of data or errors) - so be sure you do this with care.
                        """
  )
  public List<ColumnType> getColumnTypeOverrides() {
    return columnTypeOverrides;
  }
  
  /**
   * Get the defined {@link #columnTypeOverrides} as a map.
   * @return the defined {@link #columnTypeOverrides} as a map.
   */
  @JsonIgnore
  public Map<String, DataType> getColumnTypeOverrideMap() {
    return columnTypeOverrideMap;
  }
  
  /**
   * Builder class for SourceJdbc.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.JDBC;
    private String name;
    private String endpoint;
    private String endpointTemplate;
    private String query;
    private String queryTemplate;
    private int jdbcFetchSize = -1;
    private int processingBatchSize = 1000;
    private Duration connectionTimeout;
    private Boolean replaceDoubleQuotes;
    private ImmutableList<ColumnType> columnTypeOverrides;

    private Builder() {
    }

    /**
     * Set the {@link SourceJdbc#type} value in the builder.
     * @param value The value for the {@link SourceJdbc#type}, must be {@link SourceType#JDBC}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#name} value in the builder.
     * @param value The value for the {@link SourceJdbc#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#endpoint} value in the builder.
     * @param value The value for the {@link SourceJdbc#endpoint}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder endpoint(final String value) {
      this.endpoint = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#endpointTemplate} value in the builder.
     * @param value The value for the {@link SourceJdbc#endpointTemplate}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder endpointTemplate(final String value) {
      this.endpointTemplate = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#query} value in the builder.
     * @param value The value for the {@link SourceJdbc#query}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder query(final String value) {
      this.query = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#queryTemplate} value in the builder.
     * @param value The value for the {@link SourceJdbc#queryTemplate}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder queryTemplate(final String value) {
      this.queryTemplate = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#jdbcFetchSize} value in the builder.
     * @param value The value for the {@link SourceJdbc#jdbcFetchSize}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder jdbcFetchSize(final int value) {
      this.jdbcFetchSize = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#processingBatchSize} value in the builder.
     * @param value The value for the {@link SourceJdbc#processingBatchSize}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder processingBatchSize(final int value) {
      this.processingBatchSize = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#connectionTimeout} value in the builder.
     * @param value The value for the {@link SourceJdbc#connectionTimeout}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder connectionTimeout(final Duration value) {
      this.connectionTimeout = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#replaceDoubleQuotes} value in the builder.
     * @param value The value for the {@link SourceJdbc#replaceDoubleQuotes}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder replaceDoubleQuotes(final Boolean value) {
      this.replaceDoubleQuotes = value;
      return this;
    }

    /**
     * Set the {@link SourceJdbc#columnTypeOverrides} value in the builder.
     * @param value The value for the {@link SourceJdbc#columnTypeOverrides}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder columnTypeOverrides(final List<ColumnType> value) {
      this.columnTypeOverrides = ImmutableCollectionTools.copy(value);
      return this;
    }

    /**
     * Construct a new instance of the SourceJdbc class.
     * @return a new instance of the SourceJdbc class.
     */
    public SourceJdbc build() {
      return new SourceJdbc(type, name, endpoint, endpointTemplate
            , query, queryTemplate
            , jdbcFetchSize, processingBatchSize
            , connectionTimeout
            , replaceDoubleQuotes
            , columnTypeOverrides
      );
    }
  }

  /**
   * Construct a new instance of the SourceJdbc.Builder class.
   * @return a new instance of the SourceJdbc.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Constructor.
   * @param type {@link SourceJdbc#type}
   * @param name {@link SourceJdbc#name}
   * @param endpoint {@link SourceJdbc#endpoint}
   * @param endpointTemplate {@link SourceJdbc#endpointTemplate}
   * @param query {@link SourceJdbc#query}
   * @param queryTemplate {@link SourceJdbc#queryTemplate}
   * @param jdbcFetchSize {@link SourceJdbc#jdbcFetchSize}
   * @param processingBatchSize {@link SourceJdbc#processingBatchSize}
   * @param connectionTimeout {@link SourceJdbc#connectionTimeout}
   * @param replaceDoubleQuotes  {@link SourceJdbc#replaceDoubleQuotes}
   * @param columnTypeOverrides {@link SourceJdbc#columnTypeOverrides}
   */
  public SourceJdbc(final SourceType type
          , final String name
          , final String endpoint
          , final String endpointTemplate
          , final String query
          , final String queryTemplate
          , final int jdbcFetchSize
          , final int processingBatchSize
          , final Duration connectionTimeout
          , final Boolean replaceDoubleQuotes
          , final List<ColumnType> columnTypeOverrides
  ) {
    validateType(SourceType.JDBC, type);
    this.type = type;
    this.name = name;
    this.endpoint = endpoint;
    this.endpointTemplate = endpointTemplate;
    this.query = query;
    this.queryTemplate = queryTemplate;
    this.jdbcFetchSize = jdbcFetchSize;
    this.processingBatchSize = processingBatchSize;
    this.connectionTimeout = connectionTimeout;
    this.replaceDoubleQuotes = replaceDoubleQuotes;
    if (columnTypeOverrides == null || columnTypeOverrides.isEmpty()) {
      this.columnTypeOverrides = null;
      this.columnTypeOverrideMap = null;
    } else {
      ImmutableMap.Builder<String, DataType> builder = ImmutableMap.<String, DataType>builder();
      columnTypeOverrides.forEach(cto -> {
        builder.put(cto.getColumn(), cto.getType());
      });
      this.columnTypeOverrides = ImmutableCollectionTools.copy(columnTypeOverrides);
      this.columnTypeOverrideMap = builder.build();
    }
  }
}
