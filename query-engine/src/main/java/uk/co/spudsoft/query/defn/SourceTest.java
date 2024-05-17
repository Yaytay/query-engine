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
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.sources.test.SourceTestInstance;

/**
 *
 * Source producing a fixed set of data without any need to communicate with a database.
 * <P>
 * The data stream will have two fields:
 * <UL>
 * <LI>value
 * A monotonically increasing integer.
 * <LI>name
 * The name of the source.
 * </UL>
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceTest.Builder.class)
@Schema(description = """
                      Source producing a fixed set of data without any need to communicate with a database.
                      <P>
                      The data stream will have two fields:
                      <UL>
                      <LI>value
                      A monotonically increasing integer.
                      <LI>name
                      The name of the source.
                      </UL>
                      The number of rows to be returned can be configured, as can a delay between each row returned.
                      """)
public class SourceTest implements Source {

  private final SourceType type;
  private final int rowCount;
  private final String name;
  private final int delayMs;

  @Override
  public void validate() {
    validateType(SourceType.TEST, type);
    String label = name;
    if (!Strings.isNullOrEmpty(name)) {
      label = label + " ";
    }
    if (rowCount < 0) {
      throw new IllegalArgumentException(type + "Source " + label + "has negative value for rowCount");
    }
    if (rowCount == 0) {
      throw new IllegalArgumentException(type + "Source " + label + "has zero value for rowCount");
    }
    if (delayMs < 0) {
      throw new IllegalArgumentException(type + "Source " + label + "has negative value for delayMs");
    }
  }
  
  @Override
  public SourceType getType() {
    return type;
  }

  /**
   * Get the number of rows that the source will return.
   * @return the number of rows that the source will return.
   */
  @Schema(description = """
                        The number of rows that the source will return.
                        """
          , minimum = "0"
  )
  public int getRowCount() {
    return rowCount;
  }

  @Override
  public String getName() {
    return name;
  }  

  /**
   * Get the number of milliseconds to delay between production of each data row.
   * <P>
   * Note that 0 explicitly outputs all rows in a single thread and any non-zero value will use a periodic timer to output rows.
   * 
   * @return the number of milliseconds to delay between production of each data row.
   */
  @Schema(description = """
                        Get the number of milliseconds to delay between production of each data row.
                        <P>
                        Note that 0 explicitly outputs all rows in a single thread and any non-zero value will use a periodic timer to output rows.
                        """
            , minimum = "0"
)
  public int getDelayMs() {
    return delayMs;
  }
  
  @Override
  public SourceTestInstance createInstance(Vertx vertx, Context context, SharedMap sharedMap, String defaultName) {
    return new SourceTestInstance(context, this, defaultName);
  }
  
  /**
   * Builder class.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.TEST;
    private int rowCount = 1;
    private String name;
    private int delayMs;

    private Builder() {
    }

    /**
     * Construct a new {@link SourceTest} instance.
     * @return a newly created {@link SourceTest} instance.
     */
    public SourceTest build() {
      return new SourceTest(type, rowCount, name, delayMs);
    }

    /**
     * Set the {@link SourceTest#type} value on the builder.
     * @param value the type value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link SourceTest#rowCount} value on the builder.
     * @param value the rowCount value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder rowCount(final int value) {
      this.rowCount = value;
      return this;
    }

    /**
     * Set the {@link SourceTest#name} value on the builder.
     * @param value the name value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }
    
    /**
     * Set the {@link SourceTest#delayMs} value on the builder.
     * @param value the delayMs value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder delayMs(final int value) {
      this.delayMs = value;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private SourceTest(final SourceType type, final int rowCount, final String name, int delayMs) {
    validateType(SourceType.TEST, type);
    this.type = type;
    this.rowCount = rowCount;
    this.name = name;
    this.delayMs = delayMs;
  }
    
}
