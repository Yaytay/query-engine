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
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.time.format.DateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.json.FormatJsonInstance;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.context.RequestContext;

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
  private final boolean compatibleEmpty;
  private final boolean outputNullValues;
  private final int prettiness;

  @Override
  public FormatInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
    return new FormatJsonInstance(writeStream, requestContext, this);
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
   * When set to true a JSON feed that has no data will be output as an empty JSON object.
   * <P>
   * This means that no metadata will be output, and no "data" field will be output - there will be nothing but an empty object
   * regardless of the rest of the configuration.
   * <P>
   * This is only relevant if the feed has no rows to output.
   *
   * @return true if an empty result should consist of nothing but an empty JSON object.
   */
  @Schema(description = """
                        When set to true a JSON feed that has no data will be output as an empty JSON object.
                        <P>
                        This means that no metadata will be output, and no "data" field will be output - there will be nothing but an empty object
                        regardless of the rest of the configuration.
                        <P>
                        This is only relevant if the feed has no rows to output.
                        """
    , defaultValue = "false"
    , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean isCompatibleEmpty() {
    return compatibleEmpty;
  }

  /**
   * When set to true (the default) every object in a JSON feed will include every field.
   * <P>
   * The default JSON output can be very inefficient if the data contains a lot of null values.
   * By setting this to false, null fields will be omitted completely from the output.
   * <P>
   * In order to avoid confusing consumers of the stream the first row output will always contain all the fields.
   *
   * @return false if null fields should be removed from the output after the first row.
   */
  @Schema(description = """
                        When set to true (the default) every object in a JSON feed will include every field.
                        <P>
                        The default JSON output can be very inefficient if the data contains a lot of null values.
                        By setting this to false, null fields will be omitted completely from the output.
                        <P>
                        In order to avoid confusing consumers of the stream the first row output will always contain all the fields.
                        """
    , defaultValue = "true"
    , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean isOutputNullValues() {
    return outputNullValues;
  }

  /**
   * Controls the level of JSON formatting prettiness.
   * <P>
   * A value of 0 (or less) produces compact JSON output with no extra whitespace.
   * A value of 1 (the default) produces JSON with one newline character after each row.
   * A value of 2 (or more) outputs each field on a separate line and indents each field with whitespace.
   * 
   * Any values greater than 1 make use of Jackson PrettyPrinters and are inherently slower and a lot more verbose.
   * Avoid values greater than 1 for large datasets.
   * Furthermore there won't be any attempt to make values greater than 1 <em>increase</em> verbosity as numbers increase
   * - they will simply be used for different configurations of PrettyPrinter.
   *
   * The current maximum value is 2.
   * 
   * @return the prettiness level for JSON output formatting.
   */
  @Schema(description = """
                        Controls the level of JSON formatting prettiness.
                        <P>
                        A value of 0 (or less) produces compact JSON output with no extra whitespace.
                        A value of 1 (the default) produces JSON with one newline character after each row.
                        A value of 2 (or more) outputs each field on a separate line and indents each field with whitespace.
                        <P>
                        Any values greater than 1 make use of Jackson PrettyPrinters and are inherently slower and a lot more verbose.
                        Avoid values greater than 1 for large datasets.
                        Furthermore there won't be any attempt to make values greater than 1 <em>increase</em> verbosity as numbers increase
                        - they will simply be used for different configurations of PrettyPrinter.
                        <P>
                        The current maximum value is 2.
                        """
    , defaultValue = "0"
    , minimum = "0"
    , maximum = "2"
    , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public int getPrettiness() {
    return prettiness;
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
    private boolean compatibleEmpty;
    private boolean outputNullValues = true;
    private int prettiness = 1;

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
     * Set the {@link FormatJson#compatibleEmpty} value in the builder.
     * @param value The value for the {@link FormatJson#compatibleEmpty}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder compatibleEmpty(final Boolean value) {
      this.compatibleEmpty = value == null ? false : value;
      return this;
    }

    /**
     * Set the {@link FormatJson#outputNullValues} value in the builder.
     * @param value The value for the {@link FormatJson#outputNullValues}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder outputNullValues(final Boolean value) {
      this.outputNullValues = value == null ? true : value;
      return this;
    }

    /**
     * Set the {@link FormatJson#prettiness} value in the builder.
     * @param value The value for the {@link FormatJson#prettiness}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder prettiness(final Integer value) {
      this.prettiness = value == null ? 0 : value;
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
    this.compatibleEmpty = builder.compatibleEmpty;
    this.outputNullValues = builder.outputNullValues;
    this.prettiness = builder.prettiness;
  }

}
