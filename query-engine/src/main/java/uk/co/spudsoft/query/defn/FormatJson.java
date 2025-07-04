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
import java.time.format.DateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.json.FormatJsonInstance;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 * Output the data stream in JSON.
 * This format has no specific configuration options.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatJson.Builder.class)
@Schema(description = """
                      Configuration for an output format of JSON.
                      There are no formatting options for JSON output.
                      """)
public class FormatJson extends AbstractTextFormat implements Format {

  private final String dataName;
  private final String metadataName;
  private final boolean compatibleTypeNames;

  @Override
  public FormatInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    return new FormatJsonInstance(writeStream, this);
  }

  @Override
  public void validate() {
    super.validate(FormatType.JSON, "\"", "\"");
    if (!Strings.isNullOrEmpty(metadataName) && Strings.isNullOrEmpty(dataName)) {
      throw new IllegalArgumentException("metadataName is set, but dataName is not");
    }
  }
  
  /**
   * Get the extension of the format.
   * The extension is used to determine the format based upon the URL path and also to set the default filename for the content-disposition header.
   * If multiple formats have the same extension the first in the list will be used.
   * @return the extension of the format.
   */
  @Override
  @Schema(defaultValue = "json")
  public String getExtension() {
    return super.getExtension();
  }

  /**
   * Get the media type of the format.
   * The media type is used to determine the format based upon the Accept header in the request.
   * If multiple formats have the same media type the first in the list will be used.
   * The media type will also be set as the Content-Type header in the response.
   * @return the media type of the format.
   */
  @Override
  @Schema(defaultValue = "application/json")
  public MediaType getMediaType() {
    return super.getMediaType();
  }

  /**
   * The name of the parent data element in the output JSON.
   * <P>
   * JSON output consists of an array of objects, with an object for each row of the output.
   * <P>
   * By default this is the only contents in the output (the root of the JSON will be an array).
   * <P>
   * If dataName is set the output will instead be an object containing the array.
   * 
   * @return name of the parent data element in the output JSON.
   */
  @Schema(description = """
                        The name of the parent data element in the output JSON.
                        <P>
                        JSON output consists of an array of objects, with an object for each row of the output.
                        <P>
                        By default this is the only contents in the output (the root of the JSON will be an array).
                        <P>
                        If dataName is set the output will instead be an object containing the array.
                        </P>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getDataName() {
    return dataName;
  }

  /**
   * The name of the metadata element in the output JSON.
   * <P>
   * JSON output consists of an array of objects, with an object for each row of the output.
   * <P>
   * By default this is the only contents in the output (the root of the JSON will be an array) and there will be no information about the field types in the output.
   * <P>
   * If dataName and metadataName are both set the output will instead be an object containing the array and there will be an object containing a description of the output.
   * The metadata will contain two elements:
   * <UL>
   * <LI>The name of the feed.
   * <LI>An object describing the type of each field in the output.
   * </UL>
   * 
   * @return name of the parent data element in the output JSON.
   */
  @Schema(description = """
                        The name of the metadata element in the output JSON.
                        <P>
                        JSON output consists of an array of objects, with an object for each row of the output.
                        <P>
                        By default this is the only contents in the output (the root of the JSON will be an array) and there will be no information about the field types in the output.
                        <P>
                        If dataName and metadataName are both set the output will instead be an object containing the array and there will be an object containing a description of the output.
                        The metadata will contain two elements:
                        <UL>
                        <LI>The name of the feed.
                        <LI>An object describing the type of each field in the output.
                        </UL>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getMetadataName() {
    return metadataName;
  }

  /**
   * When set to true the types output in the metadata will be recorded in lowercase and with Boolean shortened to bool.
   * <P>
   * By default, no metadata is output at all, if both metadataName and dataName are set then a separate metadata list will be output.
   * This will list all the fields in the pipeline along with their data types.
   * <P>
   * By default the types will be given as the names from the {@link DataType} enum.
   * If compatibleTypeNames is true the type names will all be in lower case and boolean will be shortened to bool.
   * <p>
   * The default is to not output metadata at all, and to not change the case of type names if metadata is output.
   * 
   * @return true if the types output in the metadata structure should be in lowercase.
   */
  @Schema(description = """
                        When set to true the types output in the metadata will be recorded in lowercase and with Boolean shortened to bool.
                        <P>
                        By default, no metadata is output at all, if both metadataName and dataName are set then a separate metadata list will be output.
                        This will list all the fields in the pipeline along with their data types.
                        <P>
                        By default the types will be given as the names from the {@link DataType} enum.
                        If compatibleTypeNames is true the type names will all be in lower case and boolean will be shortened to bool.
                        <p>
                        The default is to not output metadata at all, and to not change the case of type names if metadata is output.
                        """
          , defaultValue = "false"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean isCompatibleTypeNames() {
    return compatibleTypeNames;
  }

  /**
   * Get the Java format to use for date fields.
   * 
   * @return the Java format to use for date fields.
   */
  @Override
  @Schema(defaultValue = "yyyy-mm-dd")
  public String getDateFormat() {
    return super.getDateFormat();
  }

  /**
   * Get the Java format to use for time columns.
   * <P>
   * To be processed by a Java {@link DateTimeFormatter}.
   * 
   * @return the Java format to use for time columns.
   */
  @Override
  @Schema(defaultValue = "hh:mm:ss")
  public String getTimeFormat() {
    return super.getTimeFormat();
  }

  /**
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends AbstractTextFormat.Builder<Builder>  {

    private String dataName;
    private String metadataName;
    private boolean compatibleTypeNames;
    
    private Builder() {
      super(FormatType.JSON, "json", null, "json", null, MediaType.parse("application/json"), false
              , null, null, null, null, null
      );
    }

    /**
     * Set the {@link FormatJson#dataName} value in the builder.
     * @param value The value for the {@link FormatJson#dataName}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder dataName(final String value) {
      this.dataName = value;
      return this;
    }

    /**
     * Set the {@link FormatJson#metadataName} value in the builder.
     * @param value The value for the {@link FormatJson#metadataName}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder metadataName(final String value) {
      this.metadataName = value;
      return this;
    }

    /**
     * Set the {@link FormatJson#compatibleTypeNames} value in the builder.
     * @param value The value for the {@link FormatJson#compatibleTypeNames}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder compatibleTypeNames(final Boolean value) {
      this.compatibleTypeNames = value == null ? false : value;
      return this;
    }
    
    /**
     * Construct a new instance of the FormatJson class.
     * @return a new instance of the FormatJson class.
     */
    public FormatJson build() {
      return new FormatJson(this);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static FormatJson.Builder builder() {
    return new FormatJson.Builder();
  }

  private FormatJson(Builder builder) {
    super(builder);
    validateType(FormatType.JSON, getType());
    this.dataName = builder.dataName;
    this.metadataName = builder.metadataName;
    this.compatibleTypeNames = builder.compatibleTypeNames;
  }
  
}
