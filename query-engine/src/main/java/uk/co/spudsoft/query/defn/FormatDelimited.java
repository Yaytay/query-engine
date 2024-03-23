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
  private final String extension;
  private final MediaType mediaType;
  private final boolean headerRow;
  private final String delimiter;
  private final String openQuote;
  private final String closeQuote;
  private final String escapeCloseQuote;
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
  )
  public String getName() {
    return name;
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
  )
  public String getExtension() {
    return extension;
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
  )
  public MediaType getMediaType() {
    return mediaType;
  }

  @Schema(description = """
                        If true the output will have a header row containing the field names.
                        """
          , defaultValue = "true")
  public boolean hasHeaderRow() {
    return headerRow;
  }

  @Schema(description = """
                        The delimiter between field values in the output.
                        """
          , defaultValue = ","
          , maxLength = 10
  )
  public String getDelimiter() {
    return delimiter;
  }

  @Schema(description = """
                        Any string values in the output will be prefixed by this value.
                        """
          , defaultValue = "\""
          , maxLength = 10
)
  public String getOpenQuote() {
    return openQuote;
  }

  @Schema(description = """
                        Any string values in the output will be suffixed by this value.
                        """
          , defaultValue = "\""
          , maxLength = 10
  )
  public String getCloseQuote() {
    return closeQuote;
  }

  @Schema(description = """
                        If a string value contains the close quote string it will be prefixed by this string.
                        """
          , defaultValue = "\""
          , maxLength = 10
  )
  public String getEscapeCloseQuote() {
    return escapeCloseQuote;
  }
  
  @Schema(description = """
                        Each row in the output will be suffixed by this value.
                        """
          , defaultValue = "\r\n"
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
    private String extension = "csv";
    private MediaType mediaType = MediaType.parse("text/csv");
    private boolean headerRow = true;
    private String delimiter = ",";
    private String openQuote = "\"";
    private String closeQuote = "\"";
    private String escapeCloseQuote = "\"";
    private String newline = "\r\n";

    private Builder() {
    }

    /**
     * Set the type of the Format.
     * @param value must be FormatType.JSON.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final FormatType value) {
      this.type = value;
      return this;
    }
   
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    public Builder extension(final String value) {
      this.extension = value;
      return this;
    }

    public Builder mediaType(final String value) {
      this.mediaType = MediaType.parse(value);
      return this;
    }

    public Builder headerRow(final boolean value) {
      this.headerRow = value;
      return this;
    }

    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }

    public Builder openQuote(final String value) {
      this.openQuote = value;
      return this;
    }

    public Builder closeQuote(final String value) {
      this.closeQuote = value;
      return this;
    }

    public Builder escapeCloseQuote(final String value) {
      this.escapeCloseQuote = value;
      return this;
    }

    public Builder newline(final String value) {
      this.newline = value;
      return this;
    }
    
    /**
     * Construct a new instance of the FormatJson class.
     * @return a new instance of the FormatJson class.
     */
    public FormatDelimited build() {
      return new FormatDelimited(type, name, extension, mediaType, headerRow, delimiter, openQuote, closeQuote, escapeCloseQuote, newline);
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
          , final String extension
          , final MediaType mediaType
          , final boolean headerRow
          , final String delimiter
          , final String openQuote
          , final String closeQuote
          , final String escapeCloseQuote
          , final String newline          
  ) {
    validateType(FormatType.Delimited, type);
    this.type = type;
    this.name = name;
    this.extension = extension;
    this.mediaType = mediaType;
    this.headerRow = headerRow;
    this.delimiter = delimiter;
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.escapeCloseQuote = escapeCloseQuote;
    this.newline = newline;
  }
    
  
}
