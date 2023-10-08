/*
 * Copyright (C) 2023 jtalbut
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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * A definition of a rule that prevents a pipeline from running if previous runs that match the scope and time limit exceed the 
 * byte count.
 * 
 * Note that rate limit rules are only evaluated before running a pipeline and do not take the current run into consideration at all.
 * 
 * As an example a rateLimit defined as:
 * scope: [ "username", "path" ]
 * timeLimit: PT10M
 * byteLimit: 10000000
 * says that if the current user has executed the current pipeline (same path) within the past ten minutes generating more than ten million bytes then this request should be refused.
 * 
 * Refused requests result in an HTTP status code 429 ("Too Many Requests").
 * The body of the response will indicate whether the failure was caused by a RateLimitRule or a ConcurrencyRule but will not give further details.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = RateLimitRule.Builder.class)
@Schema(description = """
                       <p>A definition of a rule that prevents a pipeline from running if previous runs that match the scope and time limit exceed the byte count.</p>
                       <p>Note that rate limit rules are only evaluated before running a pipeline and do not take the current run into consideration at all.</p>
                       <p>
                       * As an example a rateLimit defined as:
                       <pre>
                       * scope: [ "username", "path" ]
                       * timeLimit: PT10M
                       * byteLimit: 10000000
                       </pre>
                       says that if the current user has executed the current pipeline (same path) within the past ten minutes generating more than ten million bytes then this request should be refused.
                       </p>
                       <p>
                       Refused requests result in an HTTP status code 429 ("Too Many Requests").
                       The body of the response will indicate whether the failure was caused by a RateLimit or a ConcurrencyRule but will not give further details.
                       </p>
                       """
)
public class RateLimitRule {
  
  private final ImmutableList<ConcurrencyScopeType> scope;
  private final Duration timeLimit;
  private final long byteLimit;

  public void validate() {
    if (CollectionUtils.isEmpty(scope))  {
      throw new IllegalArgumentException("No scope provided for rate limit rule.");
    }
    if (timeLimit == null)  {
      throw new IllegalArgumentException("No timeLimit provided for rate limit rule.");
    }
    if (timeLimit.isNegative())  {
      throw new IllegalArgumentException("Negative timeLimit provided for rate limit rule.");
    }
    if (timeLimit.isZero())  {
      throw new IllegalArgumentException("No byteLimit timeLimit for rate limit rule.");
    }
    if (byteLimit < 0)  {
      throw new IllegalArgumentException("Negative byteLimit provided for rate limit rule.");
    }
    if (byteLimit == 0)  {
      throw new IllegalArgumentException("No byteLimit provided for rate limit rule.");
    }
  }
  
  /**
   * Get the scope of the rate limit.
   * @return the scope of the rate limit.
   */
  @ArraySchema(
          schema = @Schema(
                  implementation = ConcurrencyScopeType.class
                  , description = """
                          <P>The scope of the rate limit rule.</P>
                          <P>At least one value must be provided.</P>
                          """
          )
          , minItems = 1
          , uniqueItems = true
  )
  public List<ConcurrencyScopeType> getScope() {
    return scope;
  }

  /**
   * Get the duration of the rate limit.
   * @return the duration of the rate limit.
   */
  @Schema(description = """
                        <P>The duration of the rate limit.</P>
                        <P>Expressions in ISO8601 time period notication (e.g. PT10M for ten minutes).</P>
                        """
  , requiredMode = Schema.RequiredMode.REQUIRED)
  public Duration getTimeLimit() {
    return timeLimit;
  }

  /**
   * Get the limit on the number of bytes that may be been sent by previous runs.
   * @return the limit on the number of bytes that may be been sent by previous runs.
   */
  @Schema(description = """
                        <P>The limit on the number of bytes that may be been sent by previous runs.</P>
                        """
  , requiredMode = Schema.RequiredMode.REQUIRED)
  public long getByteLimit() {
    return byteLimit;
  }

  /**
   * Builder class for {@link Endpoint} objects.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private List<ConcurrencyScopeType> scope;
    private Duration timeLimit;
    private long byteLimit;

    private Builder() {
    }

    public Builder scope(final List<ConcurrencyScopeType> value) {
      this.scope = value;
      return this;
    }

    public Builder timeLimit(final Duration value) {
      this.timeLimit = value;
      return this;
    }

    public Builder byteLimit(final long value) {
      this.byteLimit = value;
      return this;
    }

    public RateLimitRule build() {
      return new uk.co.spudsoft.query.defn.RateLimitRule(scope, timeLimit, byteLimit);
    }
  }

  public static RateLimitRule.Builder builder() {
    return new RateLimitRule.Builder();
  }

  private RateLimitRule(final List<ConcurrencyScopeType> scope, final Duration timeLimit, final long byteLimit) {
    this.scope = ImmutableCollectionTools.copy(scope);
    this.timeLimit = timeLimit;
    this.byteLimit = byteLimit;
  }
  
  
}
