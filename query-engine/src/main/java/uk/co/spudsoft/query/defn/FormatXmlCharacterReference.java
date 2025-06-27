/*
 * Copyright (C) 2025 jtalbut
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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration for character reference replacement in XML output.
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatXmlCharacterReference.Builder.class)
@Schema(description = """
                      Configuration for character reference replacement in XML output.
                      Specifies a character or string to be replaced with a specific XML character reference.
                      """)
public class FormatXmlCharacterReference {

  private final String replace;
  private final String with;

  /**
   * Constructor for FormatXmlCharacterReference, using the Builder class for initialization.
   *
   * @param builder the Builder instance containing the configuration values.
   */
  private FormatXmlCharacterReference(Builder builder) {
    this.replace = builder.replace;
    this.with = builder.with;
  }

  /**
   * Construct a new instance of the FormatXmlCharacterReference.Builder class.
   *
   * @return a new instance of the FormatXmlCharacterReference.Builder class.
   */
  public static FormatXmlCharacterReference.Builder builder() {
    return new FormatXmlCharacterReference.Builder();
  }

  /**
   * Get the character or string to be replaced in the XML output.
   *
   * @return the character or string to be replaced.
   */
  @Schema(description = """
                        The character or string to be replaced in the XML output.
                        """,
          example = "–",
          requiredMode = Schema.RequiredMode.REQUIRED,
          maxLength = 10)
  public String getReplace() {
    return replace;
  }

  /**
   * Get the XML character reference to replace the character with.
   *
   * @return the XML character reference (without the leading &amp; and trailing ;).
   */
  @Schema(description = """
                        The XML character reference to replace the character with.
                        This must be a valid XML entity reference without the leading & and trailing ; characters.
                        For example, use "#x2013" to produce "&#x2013;" in the output.
                        """,
          example = "#x2013",
          requiredMode = Schema.RequiredMode.REQUIRED,
          maxLength = 20)
  public String getWith() {
    return with;
  }

  /**
   * Builder class for creating instances of the FormatXmlCharacterReference class.
   *
   * The Builder pattern allows for the incremental construction and customization
   * of the FormatXmlCharacterReference class by setting the replacement character
   * and the corresponding XML character reference.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String replace;
    private String with;

    /**
     * Default constructor.
     */
    private Builder() {
    }

    /**
     * Set the character or string to be replaced in the XML output.
     *
     * @param replace the character or string to be replaced.
     * @return this Builder instance.
     */
    public Builder replace(final String replace) {
      this.replace = replace;
      return this;
    }

    /**
     * Set the XML character reference to replace the character with.
     * 
     * @param with the XML character reference (without the leading &amp; and trailing ;).
     * @return this Builder instance.
     */
    public Builder with(final String with) {
      this.with = with;
      return this;
    }

    /**
     * Build the FormatXmlCharacterReference instance.
     *
     * @return a new FormatXmlCharacterReference instance with the configured values.
     */
    public FormatXmlCharacterReference build() {
      return new FormatXmlCharacterReference(this);
    }
  }
}
