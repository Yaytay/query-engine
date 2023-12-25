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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.spudsoft.xlsx.ColumnDefinition;

/**
 * Specification of the formatting of a column in XLSX output.
 * 
 * @author njt
 */
@JsonDeserialize(builder = FormatXlsxColumn.Builder.class)
@Schema(description = """
                      Specification of the formatting of a column in XLSX output.
                      """)
public class FormatXlsxColumn {
  
  private final String header;
  private final String format;
  private final Double width;

  public ColumnDefinition toColumnDefinition(String key, DataType type) {
    String calculatedHeader = Strings.isNullOrEmpty(header) ? key : header;
    String calculatedFormat = Strings.isNullOrEmpty(format) ? defaultFormatFor(type) : format;
    Double calculatedWidth = width == null ? defaultWidthFor(type, calculatedHeader, calculatedFormat): width;
    return new ColumnDefinition(calculatedHeader, calculatedFormat, calculatedWidth);
  }

  /**
   * Get the title to put in the header row instead of the field name.
   * @return the title to put in the header row instead of the field name.
   */
  @Schema(description = """
                        <P>The title to put in the header row instead of the field name.</P>
                        """
          , maxLength = 100
  )
  public String getHeader() {
    return header;
  }

  /**
   * Get the Excel format to apply to body cells instead of the default.
   * @return the Excel format to apply to body cells instead of the default.
   */
  @Schema(description = """
                        <P>The Excel format to apply to body cells instead of the default.</P>
                        <P>
                        This is an Excel format as would be entered in the Format Cells -> Number -> Custom box.
                        </P>
                        """
          , externalDocs = @ExternalDocumentation(description = "Excel format documentation", url = "https://support.microsoft.com/en-gb/office/number-format-codes-5026bbd6-04bc-48cd-bf33-80f18b4eae68")
          , maxLength = 1000
  )
  public String getFormat() {
    return format;
  }

  /**
   * Get the width of the column in Excel column width units.
   * @return the width of the column in Excel column width units.
   */
  @Schema(description = """
                        <P>The width of the column in Excel column width units.</P>
                        <P>
                        One unit of column width is equal to the width of one character in the Normal style. For proportional fonts, the width of the character 0 (zero) is used.
                        </P>
                        """
          , externalDocs = @ExternalDocumentation(description = "Additional information on Excel column widths", url = "https://learn.microsoft.com/en-us/office/troubleshoot/excel/determine-column-widths")
          , maxLength = 1000
  )
  public Double getWidth() {
    return width;
  }
    
  static String defaultFormatFor(DataType type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case Boolean -> null;
      case Date -> "yyyy-mm-dd";
      case DateTime -> "yyyy-mm-dd hh:mm:ss";
      case Double -> null;
      case Float -> null;
      case Integer -> null;
      case Long -> null;
      case String -> null;
      case Time -> "hh:mm:ss";
      default -> null;
    };
  }
  
  static int formatLength(String format) {
    if (format == null) {
      return 0;
    }
    String parts[] = format.split(";");
    int length = 0;
    for (String part : parts) {
      if (part.length() > length) {
        length = part.length();
      }
    }
    return length;
  }
  
  static Double defaultWidthFor(DataType type, String header, String format) {
    int headerLength = header.length();
    int formatLength = formatLength(format);
    int typeLength = 11;
    if (type != null) {
      typeLength = switch (type) {      
        case Boolean -> 7;
        case Date -> 11;
        case DateTime -> 20;
        case Double -> 11;
        case Float -> 11;
        case Integer -> 11;
        case Long -> 11;
        case String -> 17;
        case Time -> 9;
        default -> 0;
      };
    }
    int result = headerLength;
    if (formatLength > result) {
      result = formatLength;
    }
    if (typeLength > result) {
      result = typeLength;
    }
    return Double.valueOf(result);
  }
  
  public void validate() {
    if (Strings.isNullOrEmpty(header) && Strings.isNullOrEmpty(format) && width == null) {
      throw new IllegalArgumentException("FormatXlsxColumn has no data");
    }
    if (width != null && width < 1.0) {
      throw new IllegalArgumentException("FormatXlsxColumn has width less than 1");
    }
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String header;
    private String format;
    private Double width;

    private Builder() {
    }

    public Builder header(final String value) {
      this.header = value;
      return this;
    }

    public Builder format(final String value) {
      this.format = value;
      return this;
    }

    public Builder width(final Double value) {
      this.width = value;
      return this;
    }

    public FormatXlsxColumn build() {
      return new uk.co.spudsoft.query.defn.FormatXlsxColumn(header, format, width);
    }
  }

  public static FormatXlsxColumn.Builder builder() {
    return new FormatXlsxColumn.Builder();
  }

  private FormatXlsxColumn(final String header, final String format, final Double width) {
    this.header = header;
    this.format = format;
    this.width = width;
  }
  
}
