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
  private final String filename;
  private final MediaType mediaType;
  private final boolean hidden;

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
          , defaultValue = "html"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
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
          , defaultValue = "html"
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
          , defaultValue = "text/html"
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
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private FormatType type = FormatType.HTML;
    private String name = "html";
    private String extension = "html";
    private String filename = null;
    private MediaType mediaType = MediaType.parse("text/html");
    private boolean hidden = false;

    private Builder() {
    }

    /**
     * Set the {@link FormatHtml#type} value in the builder.
     * @param value The value for the {@link FormatHtml#type}, must be {@link FormatType#HTML}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final FormatType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link FormatHtml#name} value in the builder.
     * @param value The value for the {@link FormatHtml#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link FormatHtml#extension} value in the builder.
     * @param value The value for the {@link FormatHtml#extension}.
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
     * Set the {@link FormatHtml#mediaType} value in the builder.
     * @param value The value for the {@link FormatHtml#mediaType}.
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
     * Construct a new instance of the FormatHtml class.
     * @return a new instance of the FormatHtml class.
     */
    public FormatHtml build() {
      return new uk.co.spudsoft.query.defn.FormatHtml(type, name, extension, filename, mediaType, hidden);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static FormatHtml.Builder builder() {
    return new FormatHtml.Builder();
  }

  private FormatHtml(final FormatType type, final String name, final String extension, final String filename, final MediaType mediaType, final boolean hidden) {
    validateType(FormatType.HTML, type);
    this.type = type;
    this.name = name;
    this.extension = extension;
    this.filename = filename;
    this.mediaType = mediaType;
    this.hidden = hidden;
  }
  
}
