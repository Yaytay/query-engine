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
package uk.co.spudsoft.query.web.rest;

import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.defn.FormatType;

/**
 * An Argument represents a named piece of data that will be passed in to a pipeline.
 * Typically these correspond to query string arguments.
 * 
 * @author jtalbut
 */
@Schema(description = """
                      <P>
                      An Argument represents a named piece of data that will be passed in to a pipeline.
                      </P>
                      <P>
                      Typically these correspond to query string arguments.
                      </P>
                      """)
public final class FormatDetails {
  
  private final FormatType type;
  private final String name;
  private final String description;
  private final String extension;
  private final MediaType mediaType;

  /**
   * Constructor - create an ArgumentDetails from an Argument.
   * @param format The Argument to represent.
   */
  public FormatDetails(Format format) {

    if (format.isHidden()) {
      throw new IllegalStateException("Attempt to output hidden format");
    }
    
    this.type = format.getType();
    this.name = format.getName();
    this.description = format.getDescription();
    this.extension = format.getExtension();
    this.mediaType = format.getMediaType();
  }

  /**
   * Get the type of the format.
   *
   * @return the {@link FormatType} of this format.
   */
  @Schema(description = "The type of the format.")
  public FormatType getType() {
    return type;
  }

  /**
   * Get the name of the format, as will be used on query string parameters.
   * No two formats in a single pipeline should have the same name.
   *
   * @return the name of the format, as will be used on query string parameters.
   */
  @Schema(description = """
                        <p>The name of the format.</p>
                        <p>The name is used to determine the format based upon the '_fmt' query
                        string argument.</p>
                        <p>It is an error for two Formats to have the same name. This is different
                        from the other Format determinators which can be repeated; the name is the
                        ultimate arbiter and must be unique.</p>
                        """,
    maxLength = 100,
    defaultValue = "XML")
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
  public String getDescription() {
    return description;
  }

  /**
   * Get the extension of the format.
   *
   * @return the file extension used for this format.
   */
  @Schema(description = """
                          <p>The extension of the format.</p>
                          <p>This is used to determine the file extension for output files and
                          for URL paths.</p>
                          """,
    maxLength = 100,
    defaultValue = ".xml")
  public String getExtension() {
    return extension;
  }

  /**
   * Get the media type of this format.
   *
   * @return the {@link MediaType}, which maps to Content-Type in HTTP headers.
   */
  @Schema(description = "The media type (e.g., application/xml).",
    maxLength = 100,
    defaultValue = "application/xml")
  public MediaType getMediaType() {
    return mediaType;
  }
  
}
