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
import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.fmts.text.FormatDelimitedInstance;

/**
 * Configuration for an output format of delimited text.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatDelimited.Builder.class)
@Schema(description = """
                      Configuration for an output format of delimited text.
                      """)
public class FormatDelimited implements Format {

  private final FormatType type;
  private final String name;
  private final String description;
  private final String extension;
  private final String filename;
  private final MediaType mediaType;
  private final boolean hidden;

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
  public FormatDelimitedInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    return new FormatDelimitedInstance(this, writeStream);
  }

  @Override
  public void validate() {
    validateType(FormatType.Delimited, type);
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("Format has no name");
    }
  }
  
  @Override
  public FormatType getType() {
    return type;
  }
  
  /**
   * Get the name of the format, as will be used on query string parameters.
   * No two formats in a single pipeline should have the same name.
   * @return the name of the format, as will be used on query string parameters.
   */
  @Override
  @Schema(description = """
                        <P>The name of the format.</P>
                        <P>
                        The name is used to determine the format based upon the '_fmt' query string argument.
                        </P>
                        <P>
                        It is an error for two Formats to have the same name.
                        This is different from the other Format determinators which can be repeated, the name is the
                        ultimate arbiter and must be unique.
                        This ensures that all configured Formats can be used.
                        </P>
                        """
          , maxLength = 100
          , defaultValue = "csv"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getName() {
    return name;
  }

  /**
   * Get the description of the format, optional value to help UI users choose which format to use.
   * @return the description of the format.
   */
  @Schema(description = """
                        <P>The description of the format.</P>
                        <P>
                        The description is used in UIs to help users choose which format to use.
                        </P>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Get the extension of the format.
   * The extension is used to determine the format based upon the URL path and also to set the default filename for the content-disposition header.
   * If multiple formats have the same extension the first in the list will be used.
   * @return the extension of the format.
   */
  @Override
  @Schema(description = """
                        <P>The extension of the format.</P>
                        <P>
                        The extension is used to determine the format based upon the URL path and also to set the default filename for the content-disposition header.
                        If multiple formats have the same extension the first in the list will be used.
                        </P>
                        """
          , maxLength = 20
          , defaultValue = "csv"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getExtension() {
    return extension;
  }
  
    /**
   * Get the filename to use in the Content-Disposition header.
   * 
   * If not specified then the leaf name of the pipeline (with extension the value of {@link #getExtension()} appended) will be used.
   *
   * @return the filename of the format.
   */
  @Schema(description = """
                        <P>The filename to specify in the Content-Disposition header.</P>
                        <P>
                        If not specified then the leaf name of the pipeline (with extension the value of {@link #getExtension()} appended) will be used.
                        </P>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  @Override
  public String getFilename() {
    return filename;
  }

  /**
   * Get the media type of the format.
   * The media type is used to determine the format based upon the Accept header in the request.
   * If multiple formats have the same media type the first in the list will be used.
   * The media type will also be set as the Content-Type header in the response.
   * @return the media type of the format.
   */
  @Override
  @Schema(description = """
                        <P>The media type of the format.</P>
                        <P>
                        The media type is used to determine the format based upon the Accept header in the request.
                        If multiple formats have the same media type the first in the list will be used.
                        </P>
                        <P>
                        The media type will also be set as the Content-Type header in the response.
                        </P>
                        """
          , defaultValue = "text/csv"
          , implementation = String.class
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public MediaType getMediaType() {
    return mediaType;
  }

  @Schema(description = """
                        <P>Whether the format should be removed from the list when presented as an option to users.
                        <P>
                        This has no effect on processing and is purely a UI hint.
                        <P>
                        When hidden is true the format should removed from any UI presenting formats to the user.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , defaultValue = "false"
  )
  @Override
  public boolean isHidden() {
    return hidden;
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
          , defaultValue = ","
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
          , defaultValue = "\""
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
          , defaultValue = "\""
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
          , defaultValue = "\""
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
          , defaultValue = "\""
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
          , defaultValue = "\\r\\n"
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
  public static class Builder {

    private FormatType type = FormatType.Delimited;
    private String name = "csv";
    private String description;
    private String extension = "csv";
    private String filename = null;
    private MediaType mediaType = MediaType.parse("text/csv;charset=UTF-8");
    private boolean hidden = false;
    private boolean bom = false;
    private boolean headerRow = true;
    private boolean quoteTemporal = true;
    private String delimiter = ",";
    private String openQuote = "\"";
    private String closeQuote = "\"";
    private String escapeCloseQuote = "";
    private String replaceCloseQuote = "";
    private String newline = "\r\n";

    private Builder() {
    }

    /**
     * Set the {@link FormatDelimited#type} value in the builder.
     * @param value The value for the {@link FormatDelimited#type}, must be {@link FormatType#Delimited}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final FormatType value) {
      this.type = value;
      return this;
    }
   
    /**
     * Set the {@link FormatDelimited#name} value in the builder.
     * @param value The value for the {@link FormatDelimited#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the description of the format.
     *
     * @param description the description of the format.
     * @return this Builder instance.
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#extension} value in the builder.
     * @param value The value for the {@link FormatDelimited#extension}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder extension(final String value) {
      this.extension = value;
      return this;
    }

    /**
     * Set the filename for the format.
     *
     * @param filename the default filename for the format.
     * @return this Builder instance.
     */
    public Builder filename(String filename) {
      this.filename = filename;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#mediaType} value in the builder.
     * @param value The value for the {@link FormatDelimited#mediaType}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder mediaType(final String value) {
      this.mediaType = MediaType.parse(value);
      return this;
    }

    /**
     * Set the hidden property of the format.
     *
     * @param hidden the {@link Format#isHidden()} property of the format.
     * @return this Builder instance.
     */
    public Builder hidden(final boolean hidden) {
      this.hidden = hidden;
      return this;
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
      return new FormatDelimited(type, name, description, extension, filename, mediaType, hidden, bom, headerRow, quoteTemporal, delimiter, openQuote, closeQuote, escapeCloseQuote, replaceCloseQuote, newline);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  private FormatDelimited(final FormatType type
          , final String name
          , final String description
          , final String extension
          , final String filename
          , final MediaType mediaType
          , final boolean hidden
          , final boolean bom
          , final boolean headerRow
          , final boolean quoteTemporal
          , final String delimiter
          , final String openQuote
          , final String closeQuote
          , final String escapeCloseQuote
          , final String replaceCloseQuote
          , final String newline          
  ) {
    validateType(FormatType.Delimited, type);
    this.type = type;
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.filename = filename;
    this.mediaType = mediaType;
    this.hidden = hidden;
    this.bom = bom;
    this.headerRow = headerRow;
    this.quoteTemporal = quoteTemporal;
    this.delimiter = delimiter;
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.escapeCloseQuote = escapeCloseQuote;
    this.replaceCloseQuote = replaceCloseQuote;
    this.newline = newline;
  }
    
  
}
