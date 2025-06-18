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
import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.xml.FormatAtomInstance;
import uk.co.spudsoft.query.web.RequestContextHandler;

import static uk.co.spudsoft.query.defn.FormatXml.NAME_CHAR_REGEX;
import static uk.co.spudsoft.query.defn.FormatXml.NAME_START_REGEX;

/**
 * Output the data stream in Atom.
 * This format has no specific configuration options.
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatAtom.Builder.class)
@Schema(description = """
                      Configuration for an output format of Atom.
                      There are no formatting options for Atom output.
                      """)
public class FormatAtom extends AbstractTextFormat implements Format {

  private static final String DEFAULT_NAME = "Atom";

  private static final String DEFAULT_EXTENSION = "xml";
  private static final String DEFAULT_MEDIA_TYPE = "application/atom+xml; charset=utf-8";
  
  
  private static final String DEFAULT_FIELD_INITIAL_LETTER_FIX = "F";
  private static final String DEFAULT_FIELD_INVALID_LETTER_FIX = "_";
  
  private final String fieldInitialLetterFix;
  private final String fieldInvalidLetterFix;

  /**
   * Constructor for FormatAtom, using the Builder class for initialization.
   */
  private FormatAtom(Builder builder) {
    super(builder);
    validateType(FormatType.Atom, getType());
    this.fieldInitialLetterFix = builder.fieldInitialLetterFix;
    this.fieldInvalidLetterFix = builder.fieldInvalidLetterFix;
  }

  /**
   * Construct a new instance of the FormatAtom.Builder class.
   * @return a new instance of the FormatAtom.Builder class.
   */
  public static FormatAtom.Builder builder() {
    return new FormatAtom.Builder();
  }

  @Override
  public FormatAtomInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    RequestContext reqCtx = RequestContextHandler.getRequestContext(context);
    return new FormatAtomInstance(this, reqCtx.getPath(), writeStream);
  }

  @Override
  public void validate() {
    super.validate(FormatType.Atom, null, null);
    if (fieldInitialLetterFix != null && !NAME_START_REGEX.matcher(fieldInitialLetterFix).matches()) {
      throw new IllegalArgumentException("The value '" + fieldInitialLetterFix + "' provided as fieldInitialLetterFix is not a valid name for XML node");
    }
    if (fieldInvalidLetterFix != null && !NAME_CHAR_REGEX.matcher(fieldInvalidLetterFix).matches()) {
      throw new IllegalArgumentException("The value '" + fieldInvalidLetterFix + "' provided as fieldInvalidLetterFix is not a valid character in the name of an XML node");
    }
  }

  /**
   * Get the name of the format, as will be used on query string parameters.
   * No two formats in a single pipeline should have the same name.
   *
   * @return the name of the format, as will be used on query string parameters.
   */
  @Override
  @Schema(defaultValue = DEFAULT_NAME)
  public String getName() {
    return super.getName();
  }

  /**
   * Get the extension of the format.
   *
   * @return the file extension used for this format.
   */
  @Schema(defaultValue = DEFAULT_EXTENSION)
  @Override
  public String getExtension() {
    return super.getExtension();
  }

  /**
   * Get the media type of this format.
   *
   * @return the {@link MediaType}, which maps to Content-Type in HTTP headers.
   */
  @Schema(defaultValue = DEFAULT_MEDIA_TYPE)
  @Override
  public MediaType getMediaType() {
    return super.getMediaType();
  }

  /**
   * Get the field initial letter fix to adjust XML field names.
   *
   * @return the initial letter fix strategy.
   */
  @Schema(description = "Fix applied to the initial letter of a field's name.",
    maxLength = 100,
    defaultValue = DEFAULT_FIELD_INITIAL_LETTER_FIX
  )
  public String getFieldInitialLetterFix() {
    return fieldInitialLetterFix;
  }

  /**
   * Get the fix strategy for invalid letters in XML field names.
   *
   * @return the fix strategy for invalid letters.
   */
  @Schema(description = "Fix applied to invalid letters in field names.",
    maxLength = 100,
    defaultValue = DEFAULT_FIELD_INVALID_LETTER_FIX
  )
  public String getFieldInvalidLetterFix() {
    return fieldInvalidLetterFix;
  }

  /**
   * Builder class for creating instances of the FormatAtom class.
   *
   * The Builder pattern allows for the incremental construction and customization
   * of the FormatAtom class by setting various properties related to the format,
   * such as format type, name, extension, media type, and additional attributes specific to XML formatting.
   */
  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder extends AbstractTextFormat.Builder<Builder> {

    private String fieldInitialLetterFix = DEFAULT_FIELD_INITIAL_LETTER_FIX;
    private String fieldInvalidLetterFix = DEFAULT_FIELD_INVALID_LETTER_FIX;

    /**
     * Default constructor.
     */
    public Builder() {
      super(FormatType.Atom, DEFAULT_NAME, null, DEFAULT_EXTENSION, null, MediaType.parse(DEFAULT_MEDIA_TYPE), false
              , null, null, null, null, null
      );
    }

    /**
     * Set the strategy for fixing the initial letter of field names.
     *
     * @param fieldInitialLetterFix the initial letter fix strategy.
     * @return this Builder instance.
     */
    public Builder fieldInitialLetterFix(String fieldInitialLetterFix) {
      this.fieldInitialLetterFix = fieldInitialLetterFix;
      return this;
    }

    /**
     * Set the strategy for handling invalid letters in field names.
     *
     * @param fieldInvalidLetterFix the invalid letter fix strategy.
     * @return this Builder instance.
     */
    public Builder fieldInvalidLetterFix(String fieldInvalidLetterFix) {
      this.fieldInvalidLetterFix = fieldInvalidLetterFix;
      return this;
    }

    /**
     * Build an instance of {@link FormatAtom}.
     *
     * @return a new FormatAtom instance.
     */
    public FormatAtom build() {
      return new FormatAtom(this);
    }
  }
}
