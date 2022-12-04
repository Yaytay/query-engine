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
import uk.co.spudsoft.query.exec.fmts.html.FormatHtmlInstance;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 * The definition of an HTML output format.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatHtml.Builder.class)
@Schema(
        description = """
                      <P>The definition of an HTML output format.</P>
                      <P>
                      The HTML output format produces an HTML snippet containing a table.
                      The output itself has no formatting, but a number of CSS classes are applied to the elements enabling the UI to format them as they wish.                      
                      </P>
                      <P>
                      The CSS classes are:
                      <UL>
                      <LI>header</BR>
                      The header row.
                      <LI>dataRow</BR>
                      A row of data (other than the header row).
                      <LI>evenRow</BR>
                      An even numbered data row (the first dataRow is row 0, which is even).
                      <LI>oddRow</BR>
                      An odd numbered data row (the first dataRow is row 0, which is even).
                      <LI>evenCol</BR>
                      An even numbered column (header or dataRow, the first column is 0, which is even).
                      <LI>oddRow</BR>
                      An odd numbered column (header or dataRow, the first column is 0, which is even).
                      </UL>
                      </P>
                      """
)
public class FormatHtml implements Format {

  private final FormatType type;
  private final String name;
  private final String extension;
  private final MediaType mediaType;

  @Override
  public FormatInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    return new FormatHtmlInstance(writeStream);
  }

  @Override
  public void validate() {
    validateType(FormatType.HTML, type);
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("Format has no name");
    }
  }
    
  @Override
  public FormatType getType() {
    return type;
  }

  @Override
  @Schema(defaultValue = "html")
  public String getName() {
    return name;
  }

  @Override
  @Schema(defaultValue = "html")
  public String getExtension() {
    return extension;
  }

  @Override
  @Schema(defaultValue = "text/html", implementation = String.class)
  public MediaType getMediaType() {
    return mediaType;
  }
  
  /**
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private FormatType type = FormatType.HTML;
    private String name = "html";
    private String extension = "html";
    private MediaType mediaType = MediaType.parse("text/html");

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

    public FormatHtml build() {
      return new uk.co.spudsoft.query.defn.FormatHtml(type, name, extension, mediaType);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static FormatHtml.Builder builder() {
    return new FormatHtml.Builder();
  }

  private FormatHtml(final FormatType type, final String name, final String extension, final MediaType mediaType) {
    validateType(FormatType.HTML, type);
    this.type = type;
    this.name = name;
    this.extension = extension;
    this.mediaType = mediaType;
  }
  
}
