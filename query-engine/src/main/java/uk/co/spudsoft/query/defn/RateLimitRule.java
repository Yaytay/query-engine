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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
                       </p>
                       """
)
public class RateLimitRule {
  
  private final ImmutableList<RateLimitScopeType> scope;
  private final Duration timeLimit;
  private final String runLimit;
  private final String byteLimit;
  private final int concurrencyLimit;

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
    if (!Strings.isNullOrEmpty(runLimit)) {
      long runLimitLong = parse("run limit", runLimit);
      if (runLimitLong < 0)  {
        throw new IllegalArgumentException("Negative run limit provided for rate limit rule.");
      }
      if (runLimitLong == 0)  {
        throw new IllegalArgumentException("Zero run limit provided for rate limit rule.");
      }
    }
    if (!Strings.isNullOrEmpty(byteLimit)) {
      long byteLimitLong = parse("byte limit", byteLimit);
      if (byteLimitLong < 0)  {
        throw new IllegalArgumentException("Negative byte limit provided for rate limit rule.");
      }
      if (byteLimitLong == 0)  {
        throw new IllegalArgumentException("Zero byte limit provided for rate limit rule.");
      }
    }
    if (concurrencyLimit < 0)  {
      throw new IllegalArgumentException("Negative concurrency limit provided for rate limit rule.");
    }
  }
  
  private static long parse(String name, String value) {
    long multiplier = 1;
    if (value.endsWith("M")) {
      multiplier = 1000 * 1000;
      value = value.substring(0, value.length() - 1);
    } else if (value.endsWith("G")) {
      multiplier = 1000 * 1000 * 1000;
      value = value.substring(0, value.length() - 1);
    } else if (value.endsWith("K")) {
      multiplier = 1000;
      value = value.substring(0, value.length() - 1);
    }
    try {
      return Long.parseLong(value) * multiplier;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("The value for the " + name + " cannot be parsed");
    }
  }
  
  /**
   * Get the scope of the rate limit.
   * @return the scope of the rate limit.
   */
  @ArraySchema(
          schema = @Schema(
                  implementation = RateLimitScopeType.class
                  , description = """
                          <P>The scope of the rate limit rule.</P>
                          <P>At least one value must be provided.</P>
                          """
          )
          , minItems = 1
          , uniqueItems = true
  )
  public List<RateLimitScopeType> getScope() {
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
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
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
  public String getByteLimit() {
    return byteLimit;
  }
  
  @JsonIgnore
  public Long getParsedByteLimit() {
    if (Strings.isNullOrEmpty(byteLimit)) {
      return null;
    } else {
      return parse("byte limit", byteLimit);
    }
  }

  public String getRunLimit() {
    return runLimit;
  }

  @JsonIgnore
  public Long getParsedRunLimit() {
    if (Strings.isNullOrEmpty(runLimit)) {
      return null;
    } else {
      return parse("run limit", runLimit);
    }
  }

  public int getConcurrencyLimit() {
    return concurrencyLimit;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private List<RateLimitScopeType> scope;
    private Duration timeLimit;
    private String runLimit;
    private String byteLimit;
    private int concurrencyLimit;

    private Builder() {
    }

    public Builder scope(final List<RateLimitScopeType> value) {
      this.scope = value;
      return this;
    }

    public Builder timeLimit(final Duration value) {
      this.timeLimit = value;
      return this;
    }

    public Builder runLimit(final String value) {
      this.runLimit = value;
      return this;
    }

    public Builder byteLimit(final String value) {
      this.byteLimit = value;
      return this;
    }

    public Builder concurrencyLimit(final int value) {
      this.concurrencyLimit = value;
      return this;
    }

    public RateLimitRule build() {
      return new uk.co.spudsoft.query.defn.RateLimitRule(scope, timeLimit, runLimit, byteLimit, concurrencyLimit);
    }
  }

  public static RateLimitRule.Builder builder() {
    return new RateLimitRule.Builder();
  }

  private RateLimitRule(final List<RateLimitScopeType> scope, final Duration timeLimit, final String runLimit, final String byteLimit, final int concurrencyLimit) {
    this.scope = ImmutableCollectionTools.copy(scope);
    this.timeLimit = timeLimit;
    this.runLimit = runLimit == null ? null : runLimit.toUpperCase(Locale.ROOT);
    this.byteLimit = byteLimit == null ? null : byteLimit.toUpperCase(Locale.ROOT);
    this.concurrencyLimit = concurrencyLimit;
  }

}
