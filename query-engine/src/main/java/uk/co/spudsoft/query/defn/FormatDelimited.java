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
import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.fmts.text.FormatDelimitedInstance;

/**
 * Configuration for an output format of delimited text.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatDelimited.Builder.class)
@Schema(description = """
                      Configuration for an output format of delimited text.
                      """)
public class FormatDelimited extends AbstractTextFormat implements Format {

  private static final String DEFAULT_NAME = "csv";

  private static final String DEFAULT_EXTENSION = "csv";
  private static final String DEFAULT_MEDIA_TYPE = "text/csv;charset=UTF-8";
  
  private static final String DEFAULT_DELIMITER = ",";
  private static final String DEFAULT_NEWLINE = "\r\n";
  private static final String DEFAULT_OPEN_QUOTE = "\"";
  private static final String DEFAULT_CLOSE_QUOTE = "\"";
  
  private final boolean bom;
  private final boolean headerRow;
  private final boolean quoteTemporal;
  private final String delimiter;
  private final String openQuote;
  private final String closeQuote;
  private final String escapeCloseQuote;
  private final String replaceCloseQuote;
  private final String newline;

  @Override
  public FormatDelimitedInstance createInstance(Vertx vertx, PipelineContext pipelineContext, WriteStream<Buffer> writeStream) {
    return new FormatDelimitedInstance(this, pipelineContext, writeStream);
  }

  @Override
  public void validate() {
    super.validate(FormatType.Delimited, openQuote, closeQuote);
  }

  @Override
  @Schema(defaultValue = DEFAULT_MEDIA_TYPE)
  public MediaType getMediaType() {
    return super.getMediaType();
  }

  @Override
  @Schema(defaultValue = DEFAULT_NAME)
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(defaultValue = DEFAULT_EXTENSION)
  public String getExtension() {
    return super.getExtension();
  }

  /**
   * If true the output will have a BOM as the first byte(s) of the stream.
   * @return true true the output should have a BOM as the first byte(s) of the stream.
   */
  @Schema(description = """
                        If true the output will have a BOM as the first byte(s) of the stream.
                        """
          , defaultValue = "false"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean hasBom() {
    return bom;
  }

  /**
   * If true the output will have a header row containing the field names.
   * @return true if the output should have a header row containing the field names.
   */
  @Schema(description = """
                        If true the output will have a header row containing the field names.
                        """
          , defaultValue = "true"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean hasHeaderRow() {
    return headerRow;
  }

  /**
   * If true (the default) date/time values will be surrounded by quotes, otherwise they will not.
   * @return true if date/time values should be surrounded by quotes, otherwise they will not.
   */
  @Schema(description = """
                        If true date/time values will be surrounded by quotes, otherwise they will not.
                        """
          , defaultValue = "true"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean isQuoteTemporal() {
    return quoteTemporal;
  }

  /**
   * The delimiter between field values in the output.
   * @return the delimiter between field values in the output.
   */
  @Schema(description = """
                        The delimiter between field values in the output.
                        """
          , defaultValue = DEFAULT_DELIMITER
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getDelimiter() {
    return delimiter;
  }

  /**
   * Value with which to prefix any string values in the output.
   * @return the value with which to prefix any string values in the output.
   */
  @Schema(description = """
                        Any string values in the output will be prefixed by this value.
                        """
          , defaultValue = DEFAULT_CLOSE_QUOTE
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
)
  public String getOpenQuote() {
    return openQuote;
  }

  /**
   * Value with which to suffix any string values in the output.
   * @return the value with which to suffix any string values in the output.
   */
  @Schema(description = """
                        Any string values in the output will be suffixed by this value.
                        """
          , defaultValue = DEFAULT_OPEN_QUOTE
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getCloseQuote() {
    return closeQuote;
  }

  /**
   * Value with which to prefix any occurrence of the {@link #closeQuote} string in an output string value.
   * Do not set both this and replaceCloseQuote, this value will take preference.
   * @return the value with which to prefix any occurrence of the {@link #closeQuote} string in an output string value.
   */
  @Schema(description = """
                        If a string value contains the close quote string it will be prefixed by this string.
                        <P>
                        Do not set both this and replaceCloseQuote, this value will take preference.
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getEscapeCloseQuote() {
    return escapeCloseQuote;
  }

  /**
   * Value with which to replace any occurrence of the {@link #closeQuote} string in an output string value.
   * Do not set both this and escapeCloseQuote, the value of escapeCloseQuote will take preference.
   * @return the value with which to replace any occurrence of the {@link #closeQuote} string in an output string value.
   */
  @Schema(description = """
                        If a string value contains the close quote string it will be replaced by this string.
                        <P>
                        Do not set both this and escapeCloseQuote, the value of escapeCloseQuote will take preference.
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getReplaceCloseQuote() {
    return replaceCloseQuote;
  }

  /**
   * Value with which to suffix each row in the output.
   * @return the value with which to suffix each row in the output.
   */
  @Schema(description = """
                        Each row in the output will be suffixed by this value.
                        """
          , defaultValue = DEFAULT_NEWLINE
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getNewline() {
    return newline;
  }


  /**
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends AbstractTextFormat.Builder<Builder> {

    private boolean bom = false;
    private boolean headerRow = true;
    private boolean quoteTemporal = true;
    private String delimiter = DEFAULT_DELIMITER;
    private String openQuote = DEFAULT_OPEN_QUOTE;
    private String closeQuote = DEFAULT_CLOSE_QUOTE;
    private String escapeCloseQuote;
    private String replaceCloseQuote;
    private String newline = DEFAULT_NEWLINE;

    private Builder() {
      super(FormatType.Delimited, DEFAULT_NAME, null, DEFAULT_EXTENSION, null, null, MediaType.parse(DEFAULT_MEDIA_TYPE), false
              , null, null, null, null, null
      );
    }

    /**
     * Set the quoteTemporal property of the format.
     *
     * @param quoteTemporal the {@link FormatDelimited#isQuoteTemporal()} property of the format.
     * @return this Builder instance.
     */
    public Builder quoteTemporal(final boolean quoteTemporal) {
      this.quoteTemporal = quoteTemporal;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#bom} value in the builder.
     * @param value The value for the {@link FormatDelimited#bom}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder bom(final boolean value) {
      this.bom = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#headerRow} value in the builder.
     * @param value The value for the {@link FormatDelimited#headerRow}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder headerRow(final boolean value) {
      this.headerRow = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#delimiter} value in the builder.
     * @param value The value for the {@link FormatDelimited#delimiter}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#openQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#openQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder openQuote(final String value) {
      this.openQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#closeQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#closeQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder closeQuote(final String value) {
      this.closeQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#escapeCloseQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#escapeCloseQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder escapeCloseQuote(final String value) {
      this.escapeCloseQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#replaceCloseQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#replaceCloseQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder replaceCloseQuote(final String value) {
      this.replaceCloseQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#newline} value in the builder.
     * @param value The value for the {@link FormatDelimited#newline}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder newline(final String value) {
      this.newline = value;
      return this;
    }

    /**
     * Construct a new instance of the FormatDelimited class.
     * @return a new instance of the FormatDelimited class.
     */
    public FormatDelimited build() {
      return new FormatDelimited(this);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  private FormatDelimited(Builder builder) {
    super(builder);
    validateType(FormatType.Delimited, getType());
    this.bom = builder.bom;
    this.headerRow = builder.headerRow;
    this.quoteTemporal = builder.quoteTemporal;
    this.delimiter = builder.delimiter;
    this.openQuote = builder.openQuote;
    this.closeQuote = builder.closeQuote;
    this.escapeCloseQuote = builder.escapeCloseQuote;
    this.replaceCloseQuote = builder.replaceCloseQuote;
    this.newline = builder.newline;
  }
}
