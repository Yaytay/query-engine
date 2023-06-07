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
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.Map;
import java.util.Map.Entry;
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
public class FormatXlsx implements Format {

  private final FormatType type;
  private final String name;
  private final String extension;
  private final MediaType mediaType;
  
  private final String sheetName;
  private final String creator;
  private final boolean gridLines;
  private final boolean headers;
  private final FormatXlsxFont headerFont;
  private final FormatXlsxFont bodyFont;
  private final FormatXlsxColours headerColours;
  private final FormatXlsxColours evenColours;
  private final FormatXlsxColours oddColours;
  private final ImmutableMap<String, FormatXlsxColumn> columns;
  
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
    if (columns != null) {
      for (Entry<String, FormatXlsxColumn> entry : columns.entrySet()) {
        if (Strings.isNullOrEmpty(entry.getKey())) {
          throw new IllegalArgumentException("FormatXlsxColumn configuration has invalid name");
        }
        if (entry.getValue() == null) {
          throw new IllegalArgumentException("FormatXlsxColumn configuration is null");
        }
        entry.getValue().validate();
      }
    }
  }
  
  @Override
  public FormatType getType() {
    return type;
  }
  
  @Override
  @Schema(defaultValue = "xlsx")
  public String getName() {
    return name;
  }

  @Override
  @Schema(defaultValue = "xlsx")
  public String getExtension() {
    return extension;
  }

  @Override
  @Schema(defaultValue = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", implementation = String.class)
  public MediaType getMediaType() {
    return mediaType;
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
                        <P>
                        The key in this map is the name of the field as it appears in the data rows as they reach the outputter.
                        </P>
                        """)
  public ImmutableMap<String, FormatXlsxColumn> getColumns() {
    return columns;
  }

  /**
   * Builder class for FormatXlsx.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private FormatType type = FormatType.XLSX;
    private String name = "xlsx";
    private String extension = "xlsx";
    private MediaType mediaType = MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private String sheetName = "data";
    private String creator;
    private boolean gridLines = true;
    private boolean headers = true;
    private FormatXlsxFont headerFont;
    private FormatXlsxFont bodyFont;
    private FormatXlsxColours headerColours;
    private FormatXlsxColours evenColours;
    private FormatXlsxColours oddColours;
    private Map<String, FormatXlsxColumn> columns;

    private Builder() {
    }

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

    public Builder sheetName(final String value) {
      this.sheetName = value;
      return this;
    }

    public Builder creator(final String value) {
      this.creator = value;
      return this;
    }

    public Builder gridLines(final boolean value) {
      this.gridLines = value;
      return this;
    }

    public Builder headers(final boolean value) {
      this.headers = value;
      return this;
    }

    public Builder headerFont(final FormatXlsxFont value) {
      this.headerFont = value;
      return this;
    }

    public Builder bodyFont(final FormatXlsxFont value) {
      this.bodyFont = value;
      return this;
    }

    public Builder headerColours(final FormatXlsxColours value) {
      this.headerColours = value;
      return this;
    }

    public Builder evenColours(final FormatXlsxColours value) {
      this.evenColours = value;
      return this;
    }

    public Builder oddColours(final FormatXlsxColours value) {
      this.oddColours = value;
      return this;
    }

    public Builder columns(final Map<String, FormatXlsxColumn> value) {
      this.columns = value;
      return this;
    }

    public FormatXlsx build() {
      return new FormatXlsx(type, name, extension, mediaType, sheetName, creator, gridLines, headers, headerFont, bodyFont, headerColours, evenColours, oddColours, columns);
    }
  }


  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static FormatXlsx.Builder builder() {
    return new FormatXlsx.Builder();
  }

  private FormatXlsx(final FormatType type, final String name, final String extension, final MediaType mediaType, final String sheetName, final String creator, final boolean gridLines, final boolean headers, final FormatXlsxFont headerFont, final FormatXlsxFont bodyFont, final FormatXlsxColours headerColours, final FormatXlsxColours evenColours, final FormatXlsxColours oddColours, final Map<String, FormatXlsxColumn> columns) {
    validateType(FormatType.XLSX, type);
    this.type = type;
    this.name = name;
    this.extension = extension;
    this.mediaType = mediaType;
    this.sheetName = sheetName;
    this.creator = creator;
    this.gridLines = gridLines;
    this.headers = headers;
    this.headerFont = headerFont;
    this.bodyFont = bodyFont;
    this.headerColours = headerColours;
    this.evenColours = evenColours;
    this.oddColours = oddColours;
    this.columns = ImmutableCollectionTools.copy(columns);
  }
    
  
}
