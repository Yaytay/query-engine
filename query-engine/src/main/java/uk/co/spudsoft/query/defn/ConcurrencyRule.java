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
 * A definition of a rule that prevents a pipeline from running concurrently.
 * 
 * The time limit on concurrency rules is a get-out clause in case some error results in an incomplete request being recorded in the database.
 * 
 * As an example a concurrency defined as:
 * scope: [ "username", "path" ]
 * timeLimit: P1D
 * says that if the current user has initiated the current pipeline (same path) within the past date (P1D) without it completing then this run should be refused.
 * 
 * Refused requests result in an HTTP status code 429 ("Too Many Requests").
 * The body of the response will indicate whether the failure was caused by a RateLimitRule or a ConcurrencyRule but will not give further details.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ConcurrencyRule.Builder.class)
@Schema(description = """
                      <p>A definition of a rule that prevents a pipeline from running concurrently.</p>
                      <p>The time limit on concurrency rules is a get-out clause in case some error results in an incomplete request being recorded in the database.</p>
                      <p>
                      As an example a concurrency defined as:
                      <pre>
                      scope: [ "username", "path" ]
                      timeLimit: P1D
                      </pre>
                      says that if the current user has initiated the current pipeline (same path) within the past date (P1D) without it completing then this run should be refused.
                      </p>
                      <p>
                      Refused requests result in an HTTP status code 429 ("Too Many Requests").
                      The body of the response will indicate whether the failure was caused by a RateLimitRule or a ConcurrencyRule but will not give further details.
                      </p>
                      """
)
public class ConcurrencyRule {
  
  private final ImmutableList<ConcurrencyScopeType> scope;
  private final Duration timeLimit;

  public void validate() {
    if (CollectionUtils.isEmpty(scope))  {
      throw new IllegalArgumentException("No scope provided for concurrency rule.");
    }
    if (timeLimit == null)  {
      throw new IllegalArgumentException("No timeLimit provided for concurrency rule.");
    }
    if (timeLimit.isNegative())  {
      throw new IllegalArgumentException("Negative timeLimit provided for concurrency rule.");
    }
    if (timeLimit.isZero())  {
      throw new IllegalArgumentException("No byteLimit timeLimit for concurrency rule.");
    }
  }
  
  /**
   * Get the scope of the concurrency.
   * @return the scope of the concurrency.
   */
  @ArraySchema(
          schema = @Schema(
                  implementation = ConcurrencyScopeType.class
                  , description = """
                          <P>The scope of the concurrency rule.</P>
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
   * Get the time limit of the concurrency.
   * @return the time limit of the concurrency.
   */
  @Schema(description = """
                        <P>The time limit of the concurrency.</P>
                        <P>Expressions in ISO8601 time period notication (e.g. P1D for one day).</P>
                        """
  , requiredMode = Schema.RequiredMode.REQUIRED)
  public Duration getTimeLimit() {
    return timeLimit;
  }

  /**
   * Builder class for {@link Endpoint} objects.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private List<ConcurrencyScopeType> scope;
    private Duration timeLimit;

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

    public ConcurrencyRule build() {
      return new uk.co.spudsoft.query.defn.ConcurrencyRule(scope, timeLimit);
    }
  }

  public static ConcurrencyRule.Builder builder() {
    return new ConcurrencyRule.Builder();
  }

  private ConcurrencyRule(final List<ConcurrencyScopeType> scope, final Duration timeLimit) {
    this.scope = ImmutableCollectionTools.copy(scope);
    this.timeLimit = timeLimit;
  }
  
  
}
