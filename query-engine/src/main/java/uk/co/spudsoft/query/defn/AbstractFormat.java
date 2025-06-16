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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.net.MediaType;

/**
 * Base class for Format implementations.
 * <P>
 * There is a lot of commonality between Format implementations.
 * 
 * @author jtalbut
 */
public abstract class AbstractFormat implements Format {
  
  /**
   * The type of Format being configured, must be appropriate for the Format being configured.
   */
  protected final FormatType type;
  /**
   * The name of the format, as will be used on query string parameters.
   */
  protected final String name;
  /**
   * The description of the format, optional value to help UI users choose which format to use.
   */
  protected final String description;
  /**
   * The the extension of the format, used to determine the format based upon the URL path.
   */
  protected final String extension;
  /**
   * The filename to use in the Content-Disposition header.
   */
  protected final String filename;
  /**
   * The media type of the format, used to determine the format based upon the Accept header in the request.
   */
  protected final MediaType mediaType;
  /**
   * The hidden flag, to determine whether the format should be removed from the list when presented as an option to users.
   */
  protected final boolean hidden;

  /**
   * Constructor.
   * @param type The type of Format being configured, must be appropriate for the Format being configured.
   * @param name The name of the format, as will be used on query string parameters.
   * @param description The description of the format, optional value to help UI users choose which format to use.
   * @param extension The the extension of the format, used to determine the format based upon the URL path.
   * @param filename The filename to use in the Content-Disposition header.
   * @param mediaType The media type of the format, used to determine the format based upon the Accept header in the request.
   * @param hidden  The hidden flag, to determine whether the format should be removed from the list when presented as an option to users.
   */
  public AbstractFormat(FormatType type, String name, String description, String extension, String filename, MediaType mediaType, boolean hidden) {
    this.type = type;
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.filename = filename;
    this.mediaType = mediaType;
    this.hidden = hidden;
  }

  @Override
  public FormatType getType() {
    return type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getExtension() {
    return extension;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public MediaType getMediaType() {
    return mediaType;
  }

  @Override
  public boolean isHidden() {
    return hidden;
  }

  /**
   * Builder class for FormatJson.
   * @param <T> Subclass of Builder to use in fluent return values.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder<T extends Builder<T>> {

    /**
     * The type of Format being configured.
     */
    protected FormatType type;
    /**
     * The name of the format, as will be used on query string parameters.
     */
    protected String name;
    /**
     * The description of the format, optional value to help UI users choose which format to use.
     */
    protected String description;
    /**
     * The the extension of the format, used to determine the format based upon the URL path.
     */
    protected String extension;
    /**
     * The filename to use in the Content-Disposition header.
     */
    protected String filename;
    /**
     * The media type of the format, used to determine the format based upon the Accept header in the request.
     */
    protected MediaType mediaType;
    /**
     * The hidden flag, to determine whether the format should be removed from the list when presented as an option to users.
     */
    protected boolean hidden;

    /**
     * Constructor. 
     * @param type The type of Format being configured, must be appropriate for the Format being configured.
     * @param name The name of the format, as will be used on query string parameters.
     * @param description The description of the format, optional value to help UI users choose which format to use.
     * @param extension The the extension of the format, used to determine the format based upon the URL path.
     * @param filename The filename to use in the Content-Disposition header.
     * @param mediaType The media type of the format, used to determine the format based upon the Accept header in the request.
     * @param hidden  The hidden flag, to determine whether the format should be removed from the list when presented as an option to users.
     */
    public Builder(FormatType type, String name, String description, String extension, String filename, MediaType mediaType, boolean hidden) {
      this.type = type;
      this.name = name;
      this.description = description;
      this.extension = extension;
      this.filename = filename;
      this.mediaType = mediaType;
      this.hidden = hidden;
    }

    /**
     * This, typed to T.
     * @return this, cast to type T.
     */
    @SuppressWarnings("unchecked")
    final T self() {
        return (T) this;
    }
    
    /**
     * Set the {@link Format#getType()} value in the builder.
     * @param value The value for the {@link Format#getType()}, must match the type of the concrete class.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public T type(final FormatType value) {
      this.type = value;
      return self();
    }
   
    /**
     * Set the {@link Format#getName()} value in the builder.
     * @param value The value for the {@link Format#getName()}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public T name(final String value) {
      this.name = value;
      return self();
    }

    /**
     * Set the description of the format.
     *
     * @param description the description of the format.
     * @return this Builder instance.
     */
    public T description(String description) {
      this.description = description;
      return self();
    }

    /**
     * Set the {@link Format#getExtension()} value in the builder.
     * @param value The value for the {@link Format#getExtension()}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public T extension(final String value) {
      this.extension = value;
      return self();
    }

    /**
     * Set the filename for the format.
     *
     * @param filename the default filename for the format.
     * @return this Builder instance.
     */
    public T filename(String filename) {
      this.filename = filename;
      return self();
    }

    /**
     * Set the {@link Format#getMediaType()} value in the builder.
     * @param value The value for the {@link Format#getMediaType()}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public T mediaType(final String value) {
      this.mediaType = MediaType.parse(value);
      return self();
    }

    /**
     * Set the hidden property of the format.
     *
     * @param hidden the {@link Format#isHidden()} property of the format.
     * @return this Builder instance.
     */
    public T hidden(final boolean hidden) {
      this.hidden = hidden;
      return self();
    }
    
  }
  
}
