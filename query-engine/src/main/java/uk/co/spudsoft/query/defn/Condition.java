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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;

/**
 * Conditions are expressions using <A href="https://commons.apache.org/proper/commons-jexl/" target="_blank">JEXL</A> that control access to something.
 * 
 * <p>
 * Conditions are a single expression.
 * 
 * <P>
 * Conditions can be applied to:
 * <ul>
 * <li>entire directories (in the permissions.jexl file)
 * <li>Pipelines
 * <li>Endpoints
 * </ul>
 * <P>
 * The context of a Condition includes a variable of type {@link uk.co.spudsoft.query.exec.conditions.RequestContext} called &quot;req&quot; that includes:
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
 * The arguments will be typed according to their specified {@link uk.co.spudsoft.query.defn.ArgumentType} and will either be a single value or, if the argument is multi-valued, an {@link com.google.common.collect.ImmutableList} or typed values.
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
 * <P>
 * A condition should return either the true or false.
 * In addition if it returns the string "true" it will be considered to be true.
 * Any other return value will be considered false.
 * <P>
 * Some examples Conditions are
 * <UL>
 * <LI><pre>requestContext != null</pre>
 * Checks that the request context is not null, pretty useless in a live environment.
 * <LI><PRE>requestContext.clientIpIsIn('127.0.0.1/32','172.17.0.1/16','0:0:0:0:0:0:0:1')</PRE>
 * Checks that the client IP address is either localhost or in "172.17.0.0/16".
 * <LI><PRE>requestContext.host == 'localhost'</PRE>
 * Checks that the host on the request is localhost.
 * </UL>
 * When referencing optional arguments use "Map accesses" ({@code args['argName']}) rather than "JavaBean property access" ({@code args.argName}).
 * The dotted approach will generate an error if the argument is not supplied.
 * 
 * @author jtalbut
 */
@JsonDeserialize(using = Condition.Deserializer.class) 
@JsonSerialize(using = Condition.Serializer.class) 
@Schema(description = """
                      <P>
                      Conditions are expressions using <A href="https://commons.apache.org/proper/commons-jexl/" target="_blank">JEXL</A> that control access to something.
                      <P>
                      Conditions can be applied to entire directories (in the permissions.jexl file); to Pipelines or to Endpoints.
                      <P>
                      The context of a Condition includes a variable called &quot;req&quot; that includes:
                      <UL>
                      <LI>requestId
                      A unique ID for the request.  If Distributed Tracing is enabled this will be the Span ID, otherwise it will be a random UUID.
                      <LI>String url
                      The full URL of the request.
                      <LI>host
                      The host extracted from the URL.
                      <LI>arguments
                      A <A href="https://vertx.io/docs/apidocs/io/vertx/core/MultiMap.html" target="_blacnk">MultiMap</A> of query string arguments.
                      <LI>headers
                      A <A href="https://vertx.io/docs/apidocs/io/vertx/core/MultiMap.html" target="_blacnk">MultiMap</A> of request headers.
                      <LI>cookies
                      A map of request cookies.
                      <LI>clientIp
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
                      <P>
                      A condition should return either the boolean true or false.
                      In addition if it returns the string "true" it will be considered to be true.
                      Any other return value will be considered false.
                      <P>
                      Some examples Conditions are
                      <UL>
                      <LI><pre>requestContext != null</pre>
                      Checks that the request context is not null, pretty useless in a live environment.
                      <LI><PRE>requestContext.clientIpIsIn('127.0.0.1/32','172.17.0.1/16','0:0:0:0:0:0:0:1')</PRE>
                      Checks that the client IP address is either localhost or in "172.17.0.0/16".
                      <LI><PRE>requestContext.host == 'localhost'</PRE>
                      Checks that the host on the request is localhost.
                      </UL>
                      """)
public class Condition {
  
  private static final Logger logger = LoggerFactory.getLogger(Condition.class);
  
  private final String expression;

  /**
   * Constructor.
   * @param expression JEXL expression to be evaluated.
   */
  public Condition(String expression) {
    this.expression = expression;
  }

  /**
   * The expression that makes up the condition.
   * @return the expression that makes up the condition.
   */
  @Schema(description = """
                        The expression that makes up the condition.
                        """
          , maxLength = 1000000
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getExpression() {
    return expression;
  }
  
  /**
   * Validate the Condition.
   * <p>
   * This involves parsing the JEXL expression, but cannot validate that the reference values by it will exist at runtime.
   * 
   * @throws IllegalArgumentException if the expression cannot be parsed.
   */
  public void validate() throws IllegalArgumentException {
    try {
      createInstance();
    } catch (Throwable ex) {
      logger.debug("Failed to parse Condition expression {}: ", expression, ex);
      throw new IllegalArgumentException("Invalid condition expression: " + ex.getMessage());
    }
  }
  
  /**
   * Create a {@link uk.co.spudsoft.query.exec.conditions.ConditionInstance} for this Condition.
   * @return a newly created {@link uk.co.spudsoft.query.exec.conditions.ConditionInstance} for this Condition. 
   */
  @JsonIgnore
  public ConditionInstance createInstance() {
    return new ConditionInstance(expression);
  }
  
  /**
   * Jackson {@link com.fasterxml.jackson.databind.JsonDeserializer} for Conditions.
   */
  public static class Deserializer extends JsonDeserializer<Condition> {

    /**
     * Constructor.
     */
    public Deserializer() {
    }

    @Override
    public Condition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
      return new Condition(p.getText());
    }
    
  }
    
  /**
   * Jackson {@link com.fasterxml.jackson.databind.JsonSerializer} for Conditions.
   */
  public static class Serializer extends JsonSerializer<Condition> {  

    /**
     * Constructor.
     */
    public Serializer() {
    }

    @Override
    public void serialize(Condition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(value.getExpression());
    }
    
  }

  @Override
  public String toString() {
    return "Condition{" + "expression=" + expression + '}';
  }
       
}
