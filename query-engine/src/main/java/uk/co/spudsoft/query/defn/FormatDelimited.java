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
import uk.co.spudsoft.query.exec.fmts.CustomBooleanFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDecimalFormatter;
import uk.co.spudsoft.query.exec.fmts.text.FormatDelimitedInstance;

/**
 * Configuration for an output format of delimited text.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatDelimited.Builder.class)
@Schema(description = """
                      Configuration for an output format of delimited text.
                      """)
public class FormatDelimited extends AbstractFormat implements Format {

  private final boolean bom;
  private final boolean headerRow;
  private final boolean quoteTemporal;
  private final String delimiter;
  private final String openQuote;
  private final String closeQuote;
  private final String escapeCloseQuote;
  private final String replaceCloseQuote;
  private final String newline;

  private final String dateFormat;
  private final String dateTimeFormat;
  private final String timeFormat;
  private final String decimalFormat;
  private final String booleanFormat;
  
  @Override
  public FormatDelimitedInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream) {
    return new FormatDelimitedInstance(this, writeStream);
  }
  
  @Override
  public void validate() {
    validateType(FormatType.Delimited, type);
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("Format has no name");
    }
    if (!Strings.isNullOrEmpty(dateFormat)) {
      try {
        DateTimeFormatter.ofPattern(dateFormat);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Invalid dateFormat: " + ex.getMessage());
      }
    }
    if (!Strings.isNullOrEmpty(dateTimeFormat)) {
      try {
        new CustomDateTimeFormatter(dateTimeFormat);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Invalid dateTimeFormat: " + ex.getMessage());
      }
    }
    if (!Strings.isNullOrEmpty(timeFormat)) {
      try {
        DateTimeFormatter.ofPattern(timeFormat);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Invalid timeFormat: " + ex.getMessage());
      }
    }
    if (!Strings.isNullOrEmpty(decimalFormat)) {
      try {
        new CustomDecimalFormatter(decimalFormat);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Invalid decimalFormat: " + ex.getMessage());
      }
    }
    if (!Strings.isNullOrEmpty(booleanFormat)) {
      try {
        new CustomBooleanFormatter(booleanFormat, openQuote, closeQuote, false);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Invalid booleanFormat: " + ex.getMessage());
      }
    }
  }
  
  @Override
  @Schema(defaultValue = "text/csv;charset=UTF-8")
  public MediaType getMediaType() {
    return super.getMediaType();
  }

  @Override
  @Schema(defaultValue = "csv")
  public String getName() {
    return super.getName();
  }
  
  @Override
  @Schema(defaultValue = "csv")
  public String getExtension() {
    return super.getExtension();
  }
    
  /**
   * If true the output will have a BOM as the first byte(s) of the stream.
   * @return true true the output should have a BOM as the first byte(s) of the stream.
   */
  @Schema(description = """
                        If true the output will have a BOM as the first byte(s) of the stream.
                        """
          , defaultValue = "false"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean hasBom() {
    return bom;
  }
  
  /**
   * If true the output will have a header row containing the field names.
   * @return true if the output should have a header row containing the field names.
   */
  @Schema(description = """
                        If true the output will have a header row containing the field names.
                        """
          , defaultValue = "true"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean hasHeaderRow() {
    return headerRow;
  }

  /**
   * If true (the default) date/time values will be surrounded by quotes, otherwise they will not.
   * @return true if date/time values should be surrounded by quotes, otherwise they will not.
   */
  @Schema(description = """
                        If true date/time values will be surrounded by quotes, otherwise they will not.
                        """
          , defaultValue = "true"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public boolean isQuoteTemporal() {
    return quoteTemporal;
  }
  
  /**
   * The delimiter between field values in the output.
   * @return the delimiter between field values in the output.
   */
  @Schema(description = """
                        The delimiter between field values in the output.
                        """
          , defaultValue = ","
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getDelimiter() {
    return delimiter;
  }

  /**
   * Value with which to prefix any string values in the output.
   * @return the value with which to prefix any string values in the output.
   */
  @Schema(description = """
                        Any string values in the output will be prefixed by this value.
                        """
          , defaultValue = "\""
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
)
  public String getOpenQuote() {
    return openQuote;
  }

  /**
   * Value with which to suffix any string values in the output.
   * @return the value with which to suffix any string values in the output.
   */
  @Schema(description = """
                        Any string values in the output will be suffixed by this value.
                        """
          , defaultValue = "\""
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getCloseQuote() {
    return closeQuote;
  }

  /**
   * Value with which to prefix any occurrence of the {@link #closeQuote} string in an output string value.
   * Do not set both this and replaceCloseQuote, this value will take preference.
   * @return the value with which to prefix any occurrence of the {@link #closeQuote} string in an output string value.
   */
  @Schema(description = """
                        If a string value contains the close quote string it will be prefixed by this string.
                        <P>
                        Do not set both this and replaceCloseQuote, this value will take preference.
                        """
          , defaultValue = "\""
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getEscapeCloseQuote() {
    return escapeCloseQuote;
  }

  /**
   * Value with which to replace any occurrence of the {@link #closeQuote} string in an output string value.
   * Do not set both this and escapeCloseQuote, the value of escapeCloseQuote will take preference.
   * @return the value with which to replace any occurrence of the {@link #closeQuote} string in an output string value.
   */
  @Schema(description = """
                        If a string value contains the close quote string it will be replaced by this string.
                        <P>
                        Do not set both this and escapeCloseQuote, the value of escapeCloseQuote will take preference.
                        """
          , defaultValue = "\""
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getReplaceCloseQuote() {
    return replaceCloseQuote;
  }
  
  /**
   * Value with which to suffix each row in the output.
   * @return the value with which to suffix each row in the output.
   */
  @Schema(description = """
                        Each row in the output will be suffixed by this value.
                        """
          , defaultValue = "\\r\\n"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 10
  )
  public String getNewline() {
    return newline;
  }
  
  /**
   * Get the Java format to use for date fields.
   * <P>
   * To be processed by a Java {@link DateTimeFormatter}.
   * 
   * @return the Java format to use for date fields.
   */
  @Schema(description = """
                        The Java format to use for date fields.
                        <P>
                        This value will be used by the Java DateTimeFormatter to format dates.
                        """
          , maxLength = 100
          , defaultValue = "yyyy-MM-dd"
  )
  public String getDateFormat() {
    return dateFormat;
  }

  /**
   * Get the Java format to use for date/time columns.
   * <P>
   * To be processed by a Java {@link DateTimeFormatter}, this can either be a DateTimeFormatter pattern or one of the predefined formats.
   * <table class="striped" style="text-align:left">
   * <caption>Predefined Formatters</caption>
   * <thead>
   * <tr>
   * <th scope="col">Formatter</th>
   * <th scope="col">Description</th>
   * <th scope="col">Example</th>
   * </tr>
   * </thead>
   * <tbody>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#BASIC_ISO_DATE}</th>
   * <td>Basic ISO date </td> <td>'20111203'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_LOCAL_DATE}</th>
   * <td> ISO Local Date </td>
   * <td>'2011-12-03'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_DATE}</th>
   * <td> ISO Date with or without offset </td>
   * <td> '2011-12-03+01:00'; '2011-12-03'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_LOCAL_TIME}</th>
   * <td> Time without offset </td>
   * <td>'10:15:30'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_TIME}</th>
   * <td> Time with or without offset </td>
   * <td>'10:15:30+01:00'; '10:15:30'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}</th>
   * <td> ISO Local Date and Time </td>
   * <td>'2011-12-03T10:15:30'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_ORDINAL_DATE}</th>
   * <td> Year and day of year </td>
   * <td>'2012-337'</td>
   * </tr>
   * <tr>
   * <th scope="row"> {@link DateTimeFormatter#ISO_WEEK_DATE}</th>
   * <td> Year and Week </td>
   * <td>'2012-W48-6'</td>
   * </tr>
   * <tr>
   * <th scope="row"> EPOCH_SECONDS</th>
   * <td> Seconds since the epoch (1970-01-01)</td>
   * <td>1684158330L</td>
   * </tr>
   * <tr>
   * <th scope="row"> EPOCH_MILLISECONDS</th>
   * <td> Milliseconds since the epoch (1970-01-01)</td>
   * <td>1684158330120L</td>
   * </tr>
   *  
   * </tbody></table>
   * 
   * <table class="striped" style="text-align:left">
   * <caption>The following predefined formats all require zone/offset data that will be assumed to be UTC.</caption>
   * <thead>
   * <tr>
   * <th scope="col">Formatter</th>
   * <th scope="col">Description</th>
   * <th scope="col">Example</th>
   * </tr>
   * </thead>
   * <tbody>
   *  
   * <tr>
   * <th scope="row"> ISO_OFFSET_DATE</th>
   * <td> ISO Date with offset </td>
   * <td>'2023-05-15Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> ISO_OFFSET_TIME</th>
   * <td> Time with offset </td>
   * <td>'13:45:30.12Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> ISO_OFFSET_DATE_TIME</th>
   * <td> Date Time with Offset </td>
   * <td>'2023-05-15T13:45:30.12Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> ISO_ZONED_DATE_TIME</th>
   * <td> Zoned Date Time </td>
   * <td>'2023-05-15T13:45:30.12Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> ISO_DATE_TIME</th>
   * <td> Date and time with ZoneId </td>
   * <td>'2023-05-15T13:45:30.12Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> ISO_INSTANT</th>
   * <td> Date and Time of an Instant </td>
   * <td>'2023-05-15T13:45:30.120Z'</td>
   * </tr>
   *  
   * <tr>
   * <th scope="row"> RFC_1123_DATE_TIME</th>
   * <td> RFC 1123 / RFC 822 </td>
   * <td>'Mon, 15 May 2023 13:45:30 GMT'</td>
   * </tr>                      
   *  
   * </tbody>
   * </table>
   * <p>
   * The default output (when the format is not set) is that of {@link java.time.LocalDateTime#toString()} method, specifically, the output will be one of the following ISO-8601 formats:
   * <ul>
   * <li>uuuu-MM-dd'T'HH:mm
   * <li>uuuu-MM-dd'T'HH:mm:ss
   * <li>uuuu-MM-dd'T'HH:mm:ss.SSS
   * <li>uuuu-MM-dd'T'HH:mm:ss.SSSSSS
   * <li>uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS
   * </ul>
   * The format used will be the shortest that outputs the full value of the time where the omitted parts are implied to be zero.
   * 
   * @return the Java format to use for date/time columns.
   */
  @Schema(description = """
                        The Java format to use for date/time columns.
                        <P>
                        This value will be used by the Java DateTimeFormatter to format datetimes.
                        <P>
                        To value may be either a DateTimeFormatter pattern or one of the predefined formats:
                        <table class="striped" style="text-align:left">
                        <caption>Predefined Formatters</caption>
                        <thead>
                        <tr>
                        <th scope="col">Formatter</th>
                        <th scope="col">Description</th>
                        <th scope="col">Example</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                        <th scope="row"> BASIC_ISO_DATE</th>
                        <td>Basic ISO date </td> <td>'20111203'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_LOCAL_DATE</th>
                        <td> ISO Local Date </td>
                        <td>'2011-12-03'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_LOCAL_TIME</th>
                        <td> Time without offset </td>
                        <td>'10:15:30'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_TIME</th>
                        <td> Time with or without offset </td>
                        <td>'10:15:30+01:00'; '10:15:30'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_LOCAL_DATE_TIME</th>
                        <td> ISO Local Date and Time </td>
                        <td>'2011-12-03T10:15:30'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_ORDINAL_DATE</th>
                        <td> Year and day of year </td>
                        <td>'2012-337'</td>
                        </tr>
                        <tr>
                        <th scope="row"> EPOCH_SECONDS</th>
                        <td> Seconds since the epoch (1970-01-01)</td>
                        <td>1684158330L</td>
                        </tr>
                        <tr>
                        <th scope="row"> EPOCH_MILLISECONDS</th>
                        <td> Milliseconds since the epoch (1970-01-01)</td>
                        <td>1684158330120L</td>
                        </tr>
                        <tr colspan="3"><td>
                        The following predefined formats all require zone/offset data that will be assumed to be UTC.
                        </td></tr>
                        <tr>
                        <th scope="row"> ISO_OFFSET_DATE</th>
                        <td> ISO Date with offset </td>
                        <td>'2023-05-15Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_OFFSET_TIME</th>
                        <td> Time with offset </td>
                        <td>'13:45:30.12Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_OFFSET_DATE_TIME</th>
                        <td> Date Time with Offset </td>
                        <td>'2023-05-15T13:45:30.12Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_ZONED_DATE_TIME</th>
                        <td> Zoned Date Time </td>
                        <td>'2023-05-15T13:45:30.12Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_DATE_TIME</th>
                        <td> Date and time with ZoneId </td>
                        <td>'2023-05-15T13:45:30.12Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> ISO_INSTANT</th>
                        <td> Date and Time of an Instant </td>
                        <td>'2023-05-15T13:45:30.120Z'</td>
                        </tr>
                        <tr>
                        <th scope="row"> RFC_1123_DATE_TIME</th>
                        <td> RFC 1123 / RFC 822 </td>
                        <td>'Mon, 15 May 2023 13:45:30 GMT'</td>
                        </tr>
                        </table>
                        <P>
                        The predefined formatters have capabilities that the pattern formatting does not, specifically, if you want to output an ISO8601
                        date time with fractional seconds but only showing signficant figures in the fractional seconds, use ISO_LOCAL_DATE_TIME.
                        <P>
                        The default output (when the format is not set) is that of the java LocalDateTime.toString() method, specifically, the output will be one of the following ISO-8601 formats:
                        <ul>
                        <li>uuuu-MM-dd'T'HH:mm
                        <li>uuuu-MM-dd'T'HH:mm:ss
                        <li>uuuu-MM-dd'T'HH:mm:ss.SSS
                        <li>uuuu-MM-dd'T'HH:mm:ss.SSSSSS
                        <li>uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS
                        </ul>
                        The format used will be the shortest that outputs the full value of the time where the omitted parts are implied to be zero.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getDateTimeFormat() {
    return dateTimeFormat;
  }

  /**
   * Get the Java format to use for time columns.
   * <P>
   * To be processed by a Java {@link DateTimeFormatter}.
   * 
   * @return the Java format to use for time columns.
   */
  @Schema(description = """
                        The Java format to use for time columns.
                        <P>
                        This value will be used by the Java DateTimeFormatter to format times.
                        """
          , maxLength = 100
          , defaultValue = "HH:mm"
  )
  public String getTimeFormat() {
    return timeFormat;
  }
  
  /**
   * Get the Java {@link java.text.DecimalFormat} to use for float and double columns.
   * <P>
   * If not set the default toString() method will be called, which will result in a format equivalent to "0.0" 
   * (i.e. it will include at least one digit after the decimal point).
   * 
   * @return the Java format to use for floating point columns.
   */
  @Schema(description = """
                        The Java format to use for float and double columns.
                        <P>
                        This value will be used by the Java DecimalFormat to format floating point values.
                        <P>
                        If not set the default toString() method will be called, which will result in a format equivalent to "0.0"
                        (i.e. it will include at least one digit after the decimal point).
                        """
          , maxLength = 100
  )
  public String getDecimalFormat() {
    return decimalFormat;
  }

  /**
   * Get the format to use for Boolean columns.
   * <P>
   * This must be a <A href="https://commons.apache.org/proper/commons-jexl/" target="_blank">JEXL</A> expression that evaluates to
   * an array of two string values - the first being true and the second being false.
   * These strings will be inserted into the output stream as is, and thus must be valid, specifically they can be:
   * <UL>
   * <LI>true  (any case)
   * <LI>false  (any case)
   * <LI>A numeric value
   * <LI>A string value
   * </UL>
   * The following are all examples of valid expressions:
   * <UL>
   * <LI>['true', 'false']
   * Valid, but pointless, because this is the default behaviour.
   * <LI>['True', 'False']
   * Python formatting.
   * <LI>['1', '0']
   * Output a numeric 1 or 0.
   * <LI>['"1"', '"0"']
   * Output a quoted "1" or "0".
   * <LI>['"yes"', '"no"']
   * Output a quoted "yes" or "no".
   * </UL>
   * <P>
   * Validation is carried out on the output from the expression, but this validation is not perfect and it is possible to produce an invalid output with a bad format.
   * 
   * If not set Boolean values will be output as "true" or "false".
   * 
   * @return the format to use for Boolean columns.
   */
  @Schema(description = """
                        Get the format to use for Boolean columns.
                        <P>
                        This must be a <A href="https://commons.apache.org/proper/commons-jexl/" target="_blank">JEXL</A> expression that evaluates to
                        an array of two string values - the first being true and the second being false.
                        These strings will be inserted into the output stream as is, and thus must be valid JSON; specifically they can be:
                        <UL>
                        <LI>true  (any case)
                        <LI>false  (any case)
                        <LI>A numeric value
                        <LI>A string value
                        </UL>
                        The following are all examples of valid expressions:
                        <UL>
                        <LI>['true', 'false']
                        Valid, but pointless, because this is the default behaviour.
                        <LI>['True', 'False']
                        Python formatting.
                        <LI>['1', '0']
                        Output a numeric 1 or 0.
                        <LI>['"1"', '"0"']
                        Output a quoted "1" or "0".
                        <LI>['"yes"', '"no"']
                        Output a quoted "yes" or "no".
                        </UL>
                        <P>
                        Validation is carried out on the output from the expression, but this validation is not perfect and it is possible to produce an invalid output with a bad format.
                        <P>
                        If not set Boolean values will be output as "true" or "false".
                        """
          , maxLength = 100
  )
  public String getBooleanFormat() {
    return booleanFormat;
  }  
 
  /**
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends AbstractFormat.Builder<Builder> {

    private boolean bom = false;
    private boolean headerRow = true;
    private boolean quoteTemporal = true;
    private String delimiter = ",";
    private String openQuote = "\"";
    private String closeQuote = "\"";
    private String escapeCloseQuote = "";
    private String replaceCloseQuote = "";
    private String newline = "\r\n";
    private String dateFormat = "yyyy-MM-dd";
    private String dateTimeFormat = "yyyy-MM-dd'T'HH:mm";
    private String timeFormat = "HH:mm";
    private String decimalFormat = null;
    private String booleanFormat = null;

    private Builder() {
      super(FormatType.Delimited, "csv", null, "csv", null, MediaType.parse("text/csv;charset=UTF-8"), false);
    }

    /**
     * Set the quoteTemporal property of the format.
     *
     * @param quoteTemporal the {@link FormatDelimited#isQuoteTemporal()} property of the format.
     * @return this Builder instance.
     */
    public Builder quoteTemporal(final boolean quoteTemporal) {
      this.quoteTemporal = quoteTemporal;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#bom} value in the builder.
     * @param value The value for the {@link FormatDelimited#bom}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder bom(final boolean value) {
      this.bom = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#headerRow} value in the builder.
     * @param value The value for the {@link FormatDelimited#headerRow}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder headerRow(final boolean value) {
      this.headerRow = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#delimiter} value in the builder.
     * @param value The value for the {@link FormatDelimited#delimiter}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#openQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#openQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder openQuote(final String value) {
      this.openQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#closeQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#closeQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder closeQuote(final String value) {
      this.closeQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#escapeCloseQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#escapeCloseQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder escapeCloseQuote(final String value) {
      this.escapeCloseQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#replaceCloseQuote} value in the builder.
     * @param value The value for the {@link FormatDelimited#replaceCloseQuote}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder replaceCloseQuote(final String value) {
      this.replaceCloseQuote = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#newline} value in the builder.
     * @param value The value for the {@link FormatDelimited#newline}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder newline(final String value) {
      this.newline = value;
      return this;
    }
    
    /**
     * Set the {@link FormatDelimited#dateFormat} value in the builder.
     * @param value The value for the {@link FormatDelimited#dateFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder dateFormat(final String value) {
      this.dateFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#dateTimeFormat} value in the builder.
     * @param value The value for the {@link FormatDelimited#dateTimeFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder dateTimeFormat(final String value) {
      this.dateTimeFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#timeFormat} value in the builder.
     * @param value The value for the {@link FormatDelimited#timeFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder timeFormat(final String value) {
      this.timeFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#decimalFormat} value in the builder.
     * @param value The value for the {@link FormatDelimited#decimalFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder decimalFormat(final String value) {
      this.decimalFormat = value;
      return this;
    }

    /**
     * Set the {@link FormatDelimited#booleanFormat} value in the builder.
     * @param value The value for the {@link FormatDelimited#booleanFormat}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder booleanFormat(final String value) {
      this.booleanFormat = value;
      return this;
    }
    
    
    /**
     * Construct a new instance of the FormatDelimited class.
     * @return a new instance of the FormatDelimited class.
     */
    public FormatDelimited build() {
      return new FormatDelimited(type, name, description, extension, filename, mediaType, hidden
              , bom, headerRow, quoteTemporal, delimiter, openQuote, closeQuote, escapeCloseQuote, replaceCloseQuote, newline
              , dateFormat, dateTimeFormat, timeFormat, decimalFormat, booleanFormat);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  private FormatDelimited(final FormatType type
          , final String name
          , final String description
          , final String extension
          , final String filename
          , final MediaType mediaType
          , final boolean hidden
          , final boolean bom
          , final boolean headerRow
          , final boolean quoteTemporal
          , final String delimiter
          , final String openQuote
          , final String closeQuote
          , final String escapeCloseQuote
          , final String replaceCloseQuote
          , final String newline          
          , final String dateFormat
          , final String dateTimeFormat
          , final String timeFormat   
          , final String decimalFormat
          , final String booleanFormat
  ) {
    super(type, name, description, extension, filename, mediaType, hidden);
    validateType(FormatType.Delimited, type);
    this.bom = bom;
    this.headerRow = headerRow;
    this.quoteTemporal = quoteTemporal;
    this.delimiter = delimiter;
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.escapeCloseQuote = escapeCloseQuote;
    this.replaceCloseQuote = replaceCloseQuote;
    this.newline = newline;
    this.dateFormat = dateFormat;
    this.dateTimeFormat = dateTimeFormat;
    this.timeFormat = timeFormat;
    this.decimalFormat = decimalFormat;
    this.booleanFormat = booleanFormat;
  }
    
  
}
