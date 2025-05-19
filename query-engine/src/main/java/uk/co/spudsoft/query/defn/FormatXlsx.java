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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.List;
import uk.co.spudsoft.query.exec.fmts.xlsx.FormatXlsxInstance;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Output the data stream as a single XLSX workbook.
 * 
 * This format has a lot of additional configuration options.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatXlsx.Builder.class)
@Schema(description = """
                      Configuration for an output format of XLSX.
                      """)
public class FormatXlsx implements Format {

  private final FormatType type;
  private final String name;
  private final String description;
  private final String extension;
  private final String filename;
  private final MediaType mediaType;
  private final boolean hidden;
  
  private final String sheetName;
  private final String creator;
  private final boolean gridLines;
  private final boolean headers;
  
  private final String defaultDateFormat;
  private final String defaultDateTimeFormat;
  private final String defaultTimeFormat;
  
  private final FormatXlsxFont headerFont;
  private final FormatXlsxFont bodyFont;
  private final FormatXlsxColours headerColours;
  private final FormatXlsxColours evenColours;
  private final FormatXlsxColours oddColours;
  private final ImmutableList<FormatXlsxColumn> columns;
  private final ImmutableMap<String, FormatXlsxColumn> columnsMap;
  
  @Override
  public FormatInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    return new FormatXlsxInstance(this, writeStream);
  }

  @Override
  public void validate() {
    validateType(FormatType.XLSX, type);
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("Format has no name");
    }
    if (Strings.isNullOrEmpty(defaultDateFormat)) {
      throw new IllegalArgumentException("No default date format specified");
    }
    if (Strings.isNullOrEmpty(defaultDateTimeFormat)) {
      throw new IllegalArgumentException("No default date/time format specified");
    }
    if (Strings.isNullOrEmpty(defaultTimeFormat)) {
      throw new IllegalArgumentException("No default time format specified");
    }
    if (headerFont != null) {
      headerFont.validate();
    }
    if (bodyFont != null) {
      bodyFont.validate();
    }
    if (headerColours != null) {
      headerColours.validate();
    }
    if (evenColours != null) {
      evenColours.validate();
    }
    if (oddColours != null) {
      oddColours.validate();
    }    
    for (FormatXlsxColumn column : columns) {
      column.validate();
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
          , defaultValue = "xlsx"
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
          , defaultValue = "xlsx"
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
          , defaultValue = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          , implementation = String.class
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
   * Get the name of the sheet that will contain the data in the Excel Workbook.
   * @return the name of the sheet that will contain the data in the Excel Workbook.
   */
  @Schema(description = """
                        <P>The name of the sheet that will contain the data in the Excel Workbook.</P>
                        """
          , defaultValue = "data"
          , maxLength = 31          
  )
  public String getSheetName() {
    return sheetName;
  }

  /**
   * Get the name of the creator of the data, as it will appear in the properties of the Excel Workbook file.
   * @return the name of the creator of the data, as it will appear in the properties of the Excel Workbook file.
   */
  @Schema(description = """
                        <P>The name of the creator of the data, as it will appear in the properties of the Excel Workbook file.</P>
                        <P>
                        If no value is provided the system will attempt to extract the username from the access token used in the request.
                        If there is not value in the access token the value &quot;Unknown&quot; will be used.
                        </P>
                        """
          , maxLength = 200
  )
  public String getCreator() {
    return creator;
  }

  /**
   * Get whether or not grid lines should be shown on the Excel Worksheet.
   * @return whether or not grid lines should be shown on the Excel Worksheet.
   */
  @Schema(description = """
                        <P>Whether or not grid lines should be shown on the Excel Worksheet.</P>
                        <P>
                        If the value is true all cells in the output will have a thin black border.
                        This includes cells with a null value, but excludes cells outside the range of the data.
                        </P>
                        """
          , defaultValue = "true"
  )
  public boolean isGridLines() {
    return gridLines;
  }

  /**
   * Get whether or not a header row should be included on the Excel Worksheet.
   * @return whether or not a header row should be included on the Excel Worksheet.
   */
  @Schema(description = """
                        <P>Whether or not a header row should be included on the Excel Worksheet.</P>
                        <P>
                        If the value is true the first row on the Worksheet will contain the field names (or the overriding names from the columns defined here).
                        </P>
                        """
          , defaultValue = "true"
  )
  public boolean isHeaders() {
    return headers;
  }

  
  /**
   * Get the Excel format to use for date columns if no other format is specified.
   * @return the Excel format to use for date columns if no other format is specified.
   */
  @Schema(description = """
                        The Excel format to use for date columns if no other format is specified.
                        """
          , maxLength = 100
          , defaultValue = "yyyy-mm-dd"
  )
  public String getDefaultDateFormat() {
    return defaultDateFormat;
  }

  /**
   * Get the Excel format to use for date/time columns if no other format is specified.
   * @return the Excel format to use for date/time columns if no other format is specified.
   */
  @Schema(description = """
                        The Excel format to use for date/time columns if no other format is specified.
                        """
          , maxLength = 100
          , defaultValue = "yyyy-mm-dd hh:mm:ss"
  )
  public String getDefaultDateTimeFormat() {
    return defaultDateTimeFormat;
  }

  /**
   * Get the Excel format to use for time columns if no other format is specified.
   * @return the Excel format to use for time columns if no other format is specified.
   */
  @Schema(description = """
                        The Excel format to use for time columns if no other format is specified.
                        """
          , maxLength = 100
          , defaultValue = "hh:mm:ss"
  )
  public String getDefaultTimeFormat() {
    return defaultTimeFormat;
  }
    
  /**
   * Get the font to use for the header row.
   * @return the font to use for the header row.
   */
  @Schema(description = """
                        <P>The font to use for the header row.</P>
                        <P>
                        There is no default value in the format, but if not specified the font used will be Calibri, 11pt.
                        </P>
                        """)
  public FormatXlsxFont getHeaderFont() {
    return headerFont;
  }

  /**
   * Get the font to use for the body rows.
   * @return the font to use for the body rows.
   */
  @Schema(description = """
                        <P>The font to use for the body rows (all rows after the header row).</P>
                        <P>
                        There is no default value in the format, but if not specified the font used will be Calibri, 11pt.
                        </P>
                        """)
  public FormatXlsxFont getBodyFont() {
    return bodyFont;
  }

  /**
   * Get the foreground and background colours to use for the header row.
   * @return the foreground and background colours to use for the header row.
   */
  @Schema(description = """
                        <P>The foreground and background colours to use for the header row.</P>
                        <P>
                        There is no default value in the format, but if not specified the output will have black text on white background.
                        </P>
                        """)
  public FormatXlsxColours getHeaderColours() {
    return headerColours;
  }

  /**
   * Get the foreground and background colours to use for even numbered body rows.
   * @return the foreground and background colours to use for even numbered body rows.
   */
  @Schema(description = """
                        <P>The foreground and background colours to use for even numbered body rows.</P>
                        <P>
                        Even rows are defined to be those where the row number is even.
                        This means that if there is a header row the first data row is even, but if there is no header row then the first data row is odd.
                        </P>
                        <P>
                        There is no default value in the format, but if not specified the output will have black text on white background.
                        </P>
                        """)
  public FormatXlsxColours getEvenColours() {
    return evenColours;
  }

  /**
   * Get the foreground and background colours to use for odd numbered body rows.
   * @return the foreground and background colours to use for odd numbered body rows.
   */
  @Schema(description = """
                        <P>The foreground and background colours to use for odd numbered body rows.</P>
                        <P>
                        Odd rows are defined to be those where the row number is odd.
                        This means that if there is a header row the first data row is even, but if there is no header row then the first data row is odd.
                        </P>
                        <P>
                        There is no default value in the format, but if not specified the output will have black text on white background.
                        </P>
                        """)
  public FormatXlsxColours getOddColours() {
    return oddColours;
  }

  /**
   * Get the overrides for the formatting of specific columns.
   * @return the overrides for the formatting of specific columns.
   */
  @Schema(description = """
                        <P>The overrides for the formatting of specific columns.</P>
                        <P>
                        Usually the default formatting of a column is adequate, but this can be overridden if there is a specific need.
                        </P>
                        <P>
                        There are only three aspects of a column that can be overridden:
                        <UL>
                        <LI>The title that will appear in the header row.
                        <LI>The format that Excel will apply to the body cells.
                        <LI>The width of the column.
                        </UL>
                        </P>
                        <P>
                        There is no capability for changing the order of output columns, this will always be set as the order they appear in the data.
                        </P>
                        """)
  public List<FormatXlsxColumn> getColumns() {
    return columns;
  }

  /**
   * Get the defined {@link #columns} as a map.
   * @return the defined {@link #columns} as a map.
   */
  @JsonIgnore
  public ImmutableMap<String, FormatXlsxColumn> getColumnsMap() {
    return columnsMap;
  }
  
  /**
   * Builder class for FormatXlsx.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private FormatType type = FormatType.XLSX;
    private String name = "xlsx";
    private String description;
    private String extension = "xlsx";
    private String filename = null;
    private MediaType mediaType = MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private boolean hidden = false;

    private String sheetName = "data";
    private String creator;
    private boolean gridLines = true;
    private boolean headers = true;
    private String defaultDateFormat = "yyyy-mm-dd";
    private String defaultDateTimeFormat = "yyyy-mm-dd hh:mm:ss";
    private String defaultTimeFormat = "hh:mm:ss";
    private FormatXlsxFont headerFont;
    private FormatXlsxFont bodyFont;
    private FormatXlsxColours headerColours;
    private FormatXlsxColours evenColours;
    private FormatXlsxColours oddColours;
    private List<FormatXlsxColumn> columns;

    private Builder() {
    }

    /**
     * Set the {@link FormatXlsx#type} value in the builder.
     * @param value The value for the {@link FormatXlsx#type}, must be {@link FormatType#XLSX}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final FormatType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#name} value in the builder.
     * @param value The value for the {@link FormatXlsx#name}.
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
     * Set the {@link FormatXlsx#extension} value in the builder.
     * @param value The value for the {@link FormatXlsx#extension}.
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
     * Set the {@link FormatXlsx#mediaType} value in the builder.
     * @param value The value for the {@link FormatXlsx#mediaType}.
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
     * Set the {@link FormatXlsx#sheetName} value in the builder.
     * @param value The value for the {@link FormatXlsx#sheetName}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder sheetName(final String value) {
      this.sheetName = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#creator} value in the builder.
     * @param value The value for the {@link FormatXlsx#creator}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder creator(final String value) {
      this.creator = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#gridLines} value in the builder.
     * @param value The value for the {@link FormatXlsx#gridLines}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder gridLines(final boolean value) {
      this.gridLines = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#headers} value in the builder.
     * @param value The value for the {@link FormatXlsx#headers}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder headers(final boolean value) {
      this.headers = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#defaultDateFormat} value in the builder.
     * @param value The value for the {@link FormatXlsx#defaultDateFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder defaultDateFormat(final String value) {
      this.defaultDateFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#defaultDateTimeFormat} value in the builder.
     * @param value The value for the {@link FormatXlsx#defaultDateTimeFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder defaultDateTimeFormat(final String value) {
      this.defaultDateTimeFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#defaultTimeFormat} value in the builder.
     * @param value The value for the {@link FormatXlsx#defaultTimeFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder defaultTimeFormat(final String value) {
      this.defaultTimeFormat = value;
      return this;
    }
    
    /**
     * Set the {@link FormatXlsx#headerFont} value in the builder.
     * @param value The value for the {@link FormatXlsx#headerFont}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder headerFont(final FormatXlsxFont value) {
      this.headerFont = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#bodyFont} value in the builder.
     * @param value The value for the {@link FormatXlsx#bodyFont}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder bodyFont(final FormatXlsxFont value) {
      this.bodyFont = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#headerColours} value in the builder.
     * @param value The value for the {@link FormatXlsx#headerColours}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder headerColours(final FormatXlsxColours value) {
      this.headerColours = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#evenColours} value in the builder.
     * @param value The value for the {@link FormatXlsx#evenColours}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder evenColours(final FormatXlsxColours value) {
      this.evenColours = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#oddColours} value in the builder.
     * @param value The value for the {@link FormatXlsx#oddColours}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder oddColours(final FormatXlsxColours value) {
      this.oddColours = value;
      return this;
    }

    /**
     * Set the {@link FormatXlsx#columns} value in the builder.
     * @param value The value for the {@link FormatXlsx#columns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder columns(final List<FormatXlsxColumn> value) {
      this.columns = value;
      return this;
    }

    /**
     * Construct a new instance of the FormatXlsx class.
     * @return a new instance of the FormatXlsx class.
     */
    public FormatXlsx build() {
      return new FormatXlsx(type, name, description, extension, filename, mediaType, hidden, sheetName, creator
              , gridLines, headers, defaultDateFormat, defaultDateTimeFormat, defaultTimeFormat
              , headerFont, bodyFont, headerColours, evenColours, oddColours, columns);
    }
  }


  /**
   * Construct a new instance of the FormatXlsx.Builder class.
   * @return a new instance of the FormatXlsx.Builder class.
   */
  public static FormatXlsx.Builder builder() {
    return new FormatXlsx.Builder();
  }

  private FormatXlsx(final FormatType type, final String name, final String description, final String extension, final String filename, final MediaType mediaType, final boolean hidden
          , final String sheetName, final String creator
          , final boolean gridLines, final boolean headers, final String defaultDateFormat, final String defaultDateTimeFormat, final String defaultTimeFormat
          , final FormatXlsxFont headerFont, final FormatXlsxFont bodyFont, final FormatXlsxColours headerColours, final FormatXlsxColours evenColours, final FormatXlsxColours oddColours, final List<FormatXlsxColumn> columns) {
    validateType(FormatType.XLSX, type);
    this.type = type;
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.filename = filename;
    this.mediaType = mediaType;
    this.hidden = hidden;
    this.sheetName = sheetName;
    this.creator = creator;
    this.gridLines = gridLines;
    this.headers = headers;
    this.defaultDateFormat = defaultDateFormat;
    this.defaultDateTimeFormat = defaultDateTimeFormat;
    this.defaultTimeFormat = defaultTimeFormat;
    this.headerFont = headerFont;
    this.bodyFont = bodyFont;
    this.headerColours = headerColours;
    this.evenColours = evenColours;
    this.oddColours = oddColours;
    this.columns = ImmutableCollectionTools.copy(columns);
    this.columnsMap = ImmutableCollectionTools.listToMap(columns, c -> c.getName());
  }  
  
}
