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
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.script.ProcessorExpressionInstance;

/**
 * Processor that runs a JEXL expression that can either be a predicate for the entire row, or that can set a field value (or both).
 * <p>
 * A field may be set to any data type, but may not change type - it may be more reliable to always create a new field rather than to try to change an existing field.
 * <P>
 * The difference between the ProcessorExpression and the{@link ProcessorExpression} is that the evaluation of JEXL expressions is a lot faster that the evaluation of JavaScript,
 * but there are fewer things that can be done in a single JEXL expression.
 * In general, prefer ProcessorExpression unless it is unable to achieve what you want.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorExpression.Builder.class)
@Schema(description = """
                      Run an JEXL expression each row of the output.
                      """
)
public class ProcessorExpression implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final String id;
  private final String predicate;
  private final String field;
  private final DataType fieldType;
  private final String fieldValue;

  @Override
  public ProcessorExpressionInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorExpressionInstance(vertx, sourceNameTracker, context, this);
  }
  
  @Override
  public void validate() {
    validateType(ProcessorType.EXPRESSION, type);
    if (!Strings.isNullOrEmpty(field)) {
      if (Strings.isNullOrEmpty(fieldValue)) {
        throw new IllegalArgumentException("Processor has field set but does not have a fieldValue set");
      }
      if (fieldType == null) {
        throw new IllegalArgumentException("Processor has field set but does not have a fieldType set");
      }
    } else if (!Strings.isNullOrEmpty(fieldValue)) {
      throw new IllegalArgumentException("Processor has fieldValue set but does not have a field set");
    } else if (Strings.isNullOrEmpty(predicate)) {
      throw new IllegalArgumentException("Processor does not have field or predicate set");
    }
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public String getId() {
    return id;
  }

  /**
   * A JEXL expression that is used to determine whether or not the row should be discarded.
   * <P>
   * The script should return a value that is either true or false, if the value is false the row will be discarded.
   * <p>
   * The context of the evaluation includes a variable of type {@link uk.co.spudsoft.query.exec.conditions.RequestContext} called &quot;req&quot; that includes:
   * <UL>
   * <LI>requestId
   * A unique ID for the request.  If Distributed Tracing is enabled this will be the Span ID, otherwise it will be a random UUID.
   * <LI>String url
   * The full URL of the request.
   * <LI>host
   * The host extracted from the URL.
   * <LI>arguments
   * <p>
   * An {@link com.google.common.collect.ImmutableMap} of query string arguments.
   * <p>
   * The arguments will be typed according to their specified {@link uk.co.spudsoft.query.defn.DataType} and will either be a single value or, if the argument is multi-valued, an {@link com.google.common.collect.ImmutableList} or typed values.
   * <p>
   * Note that the arguments are only typed when a pipeline instance is created, if the arguments field is access before (i.e. in a folder permissions.jexl file) all values will be strings.
   * <LI>params
   * <p>
   * A {@link io.vertx.core.MultiMap} of un-processed query string argumets.
   * <LI>headers
   * <p>
   * A {@link io.vertx.core.MultiMap} of request headers.
   * <LI>cookies
   * <p>
   * A {@link java.util.Map} map of request cookies.
   * <LI>clientIp
   * <p>
   * The IP address of client making the request, taken from the first of:
   * <UL>
   * <LI>The X-Cluster-Client-IP header.
   * <LI>The X-Forwarded-For header.
   * <LI>The actual IP address making the TCP connection.
   * </UL>
   * <LI>jwt
   * The <A href="https://jwt.io/" target="_blank">Json Web Token</A> associated with the request, if any.
   * <LI>clientIpIsIn
   * A function that receives an array of IP addresses or subnets (in slash notation) and returns true if the clientIp matches any of them.
   * </UL>
   * 
   * @return a JEXL expression that is used to determine whether or not the row should be discarded.
   */
  @Schema(description = """
                        A JEXL expression that is used to determine whether or not the row should be discarded.
                        <P>
                        The script should return a value that is either true or false, if the value is false the row will be discarded.
                        <p>
                        The context of the evaluation includes a variable of type {@link uk.co.spudsoft.query.exec.conditions.RequestContext} called &quot;req&quot; that includes:
                        <UL>
                        <LI>requestId
                        A unique ID for the request.  If Distributed Tracing is enabled this will be the Span ID, otherwise it will be a random UUID.
                        <LI>String url
                        The full URL of the request.
                        <LI>host
                        The host extracted from the URL.
                        <LI>arguments
                        <p>
                        An {@link com.google.common.collect.ImmutableMap} of query string arguments.
                        <p>
                        The arguments will be typed according to their specified {@link uk.co.spudsoft.query.defn.DataType} and will either be a single value or, if the argument is multi-valued, an {@link com.google.common.collect.ImmutableList} or typed values.
                        <p>
                        Note that the arguments are only typed when a pipeline instance is created, if the arguments field is access before (i.e. in a folder permissions.jexl file) all values will be strings.
                        <LI>params
                        <p>
                        A {@link io.vertx.core.MultiMap} of un-processed query string argumets.
                        <LI>headers
                        <p>
                        A {@link io.vertx.core.MultiMap} of request headers.
                        <LI>cookies
                        <p>
                        A {@link java.util.Map} map of request cookies.
                        <LI>clientIp
                        <p>
                        The IP address of client making the request, taken from the first of:
                        <UL>
                        <LI>The X-Cluster-Client-IP header.
                        <LI>The X-Forwarded-For header.
                        <LI>The actual IP address making the TCP connection.
                        </UL>
                        <LI>jwt
                        The <A href="https://jwt.io/" target="_blank">Json Web Token</A> associated with the request, if any.
                        <LI>clientIpIsIn
                        A function that receives an array of IP addresses or subnets (in slash notation) and returns true if the clientIp matches any of them.
                        </UL>
                        """
          , maxLength = 1000000
  )
  public String getPredicate() {
    return predicate;
  }

  /**
   * The name of a single field that is to be created/updated.
   * @return the name of a single field that is to be created/updated.
   */
  @Schema(description = """
                        The field that is to be created/updated by the {@link Processor}.
                        <P>
                        If this value is set the fieldType and fieldValue must both also be set.
                        """
          , maxLength = 100
  )
  public String getField() {
    return field;
  }

  /**
   * The type of the field that is to be created/updated.
   * <p>
   * It can be awkward to ensure the correct return type from a JEXL expression, so the result of the expression will be cast/parsed to this data type as appropriate.
   * If the field being set is an existing field then the type specified must be the same as the existing type of that field (or must be null).
   * <p>
   * If this value is set, but "field" is not set then it will be ignored.
   * 
   * @return the type of the field that is to be created/updated.
   */
  @Schema(description = """
                        The type of the field that is to be created/updated.
                        <p>
                        It can be awkward to ensure the correct return type from a JEXL expression, so the result of the expression will be cast/parsed to this data type as appropriate.
                        If the field being set is an existing field then the type specified must be the same as the existing type of that field (or must be null).
                        <p>
                        If this value is set, but "field" is not set then it will be ignored.
                        """
  )
  public DataType getFieldType() {
    return fieldType;
  }

  /**
   * A JEXL expression that is evaluated, then parsed/cast to the fieldType and then assigned to the field.
   * <p>
   * The context of the evaluation includes a variable of type {@link uk.co.spudsoft.query.exec.conditions.RequestContext} called &quot;req&quot; that includes:
   * <UL>
   * <LI>requestId
   * A unique ID for the request.  If Distributed Tracing is enabled this will be the Span ID, otherwise it will be a random UUID.
   * <LI>String url
   * The full URL of the request.
   * <LI>host
   * The host extracted from the URL.
   * <LI>arguments
   * <p>
   * An {@link com.google.common.collect.ImmutableMap} of query string arguments.
   * <p>
   * The arguments will be typed according to their specified {@link uk.co.spudsoft.query.defn.DataType} and will either be a single value or, if the argument is multi-valued, an {@link com.google.common.collect.ImmutableList} or typed values.
   * <p>
   * Note that the arguments are only typed when a pipeline instance is created, if the arguments field is access before (i.e. in a folder permissions.jexl file) all values will be strings.
   * <LI>params
   * <p>
   * A {@link io.vertx.core.MultiMap} of un-processed query string argumets.
   * <LI>headers
   * <p>
   * A {@link io.vertx.core.MultiMap} of request headers.
   * <LI>cookies
   * <p>
   * A {@link java.util.Map} map of request cookies.
   * <LI>clientIp
   * <p>
   * The IP address of client making the request, taken from the first of:
   * <UL>
   * <LI>The X-Cluster-Client-IP header.
   * <LI>The X-Forwarded-For header.
   * <LI>The actual IP address making the TCP connection.
   * </UL>
   * <LI>jwt
   * The <A href="https://jwt.io/" target="_blank">Json Web Token</A> associated with the request, if any.
   * <LI>clientIpIsIn
   * A function that receives an array of IP addresses or subnets (in slash notation) and returns true if the clientIp matches any of them.
   * </UL>
   * 
   * @return a JEXL expression that is evaluated, then parsed/cast to the fieldType and then assigned to the field.
   */
  @Schema(description = """
                        A JEXL expression that is evaluated, then parsed/cast to the fieldType and then assigned to the field.
                        <P>
                        The context of the evaluation includes a variable of type {@link uk.co.spudsoft.query.exec.conditions.RequestContext} called &quot;req&quot; that includes:
                        <UL>
                        <LI>requestId
                        A unique ID for the request.  If Distributed Tracing is enabled this will be the Span ID, otherwise it will be a random UUID.
                        <LI>String url
                        The full URL of the request.
                        <LI>host
                        The host extracted from the URL.
                        <LI>arguments
                        <p>
                        An {@link com.google.common.collect.ImmutableMap} of query string arguments.
                        <p>
                        The arguments will be typed according to their specified {@link uk.co.spudsoft.query.defn.DataType} and will either be a single value or, if the argument is multi-valued, an {@link com.google.common.collect.ImmutableList} or typed values.
                        <p>
                        Note that the arguments are only typed when a pipeline instance is created, if the arguments field is access before (i.e. in a folder permissions.jexl file) all values will be strings.
                        <LI>params
                        <p>
                        A {@link io.vertx.core.MultiMap} of un-processed query string argumets.
                        <LI>headers
                        <p>
                        A {@link io.vertx.core.MultiMap} of request headers.
                        <LI>cookies
                        <p>
                        A {@link java.util.Map} map of request cookies.
                        <LI>clientIp
                        <p>
                        The IP address of client making the request, taken from the first of:
                        <UL>
                        <LI>The X-Cluster-Client-IP header.
                        <LI>The X-Forwarded-For header.
                        <LI>The actual IP address making the TCP connection.
                        </UL>
                        <LI>jwt
                        The <A href="https://jwt.io/" target="_blank">Json Web Token</A> associated with the request, if any.
                        <LI>clientIpIsIn
                        A function that receives an array of IP addresses or subnets (in slash notation) and returns true if the clientIp matches any of them.
                        </UL>
                        """
          , maxLength = 1000000
  )
  public String getFieldValue() {
    return fieldValue;
  }

  
  /**
   * Builder class for ProcessorExpression.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.EXPRESSION;
    private Condition condition;
    private String id;
    private String predicate;
    private String field;
    private DataType fieldType = DataType.String;
    private String fieldValue;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorExpression#type} value in the builder.
     * @param value The value for the {@link ProcessorExpression#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorExpression#condition} value in the builder.
     * @param value The value for the {@link ProcessorExpression#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorExpression#id} value in the builder.
     * @param value The value for the {@link ProcessorExpression#id}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder id(final String value) {
      this.id = value;
      return this;
    }

    /**
     * Set the {@link ProcessorExpression#predicate} value in the builder.
     * @param value The value for the {@link ProcessorExpression#predicate}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder predicate(final String value) {
      this.predicate = value;
      return this;
    }

    /**
     * Set the {@link ProcessorExpression#field} value in the builder.
     * @param value The value for the {@link ProcessorExpression#field}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder field(final String value) {
      this.field = value;
      return this;
    }

    /**
     * Set the {@link ProcessorExpression#fieldType} value in the builder.
     * @param value The value for the {@link ProcessorExpression#fieldType}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldType(final DataType value) {
      this.fieldType = value;
      return this;
    }

    /**
     * Set the {@link ProcessorExpression#fieldValue} value in the builder.
     * @param value The value for the {@link ProcessorExpression#fieldValue}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldValue(final String value) {
      this.fieldValue = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorExpression class.
     * @return a new instance of the ProcessorExpression class.
     */
    public ProcessorExpression build() {
      ProcessorExpression result = new ProcessorExpression(type, condition, id, predicate, field, fieldType, fieldValue);
      result.validateType(ProcessorType.EXPRESSION, type);
      return result;
    }
  }

  /**
   * Construct a new instance of the ProcessorExpression.Builder class.
   * @return a new instance of the ProcessorExpression.Builder class.
   */
  public static ProcessorExpression.Builder builder() {
    return new ProcessorExpression.Builder();
  }

  private ProcessorExpression(ProcessorType type, Condition condition, String id, String predicate, String field, DataType fieldType, String fieldValue) {
    this.type = type;
    this.condition = condition;
    this.id = id;
    this.predicate = predicate;
    this.field = field;
    this.fieldType = fieldType;
    this.fieldValue = fieldValue;
  }
  
  
  
  
  
  
}
