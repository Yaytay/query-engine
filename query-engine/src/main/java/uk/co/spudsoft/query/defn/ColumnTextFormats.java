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
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomBooleanFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDateTimeFormatter;
import uk.co.spudsoft.query.exec.fmts.CustomDecimalFormatter;

/**
 * Base class for Format implementations that use standard text formatters for columns.
 * <P>
 * There is a lot of commonality between Format implementations.
 *
 * @author jtalbut
 */
@Schema(
        description="""
                    <P>The overrides for the formatting of specific columns.</P>
                    <P>
                    This is only required when two columns of the same type need to be formatted in different ways.
                    </P>
                    <P>
                    Given that a column can only be of one data type it is usually only appropriate to set one format
                    with this structure for a given column.
                    </P>
                    <P>
                    Any column not specified here will have the default format, so it is only necessary to specify the
                    odd columns here.
                    </P>
                    """
)
@JsonDeserialize(builder = ColumnTextFormats.Builder.class)
public class ColumnTextFormats {

  private final String column;
  private final String dateFormat;
  private final String dateTimeFormat;
  private final String timeFormat;
  private final String decimalFormat;
  private final String booleanFormat;

  /**
   * Validate the configured data.
   * @param openQuote The quoting string that may be included at the beginning of a Boolean value, may be null.
   * @param closeQuote The quoting string that may be included at the end of a Boolean value, may be null.
   * 
   */
  protected void validate(String openQuote, String closeQuote) {
    if (Strings.isNullOrEmpty(column)) {
      throw new IllegalArgumentException("No column specified for column value formatters");
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
    if (Strings.isNullOrEmpty(dateFormat) && Strings.isNullOrEmpty(dateTimeFormat) && Strings.isNullOrEmpty(timeFormat) && Strings.isNullOrEmpty(decimalFormat) && Strings.isNullOrEmpty(booleanFormat)) {
      throw new IllegalArgumentException("No formats specified for column \"" + column + "\"");
    }
  }

  /**
   * Get the column to be formatted.
   * @return the column to be formatted.
   */
  @Schema(description = """
                        The column to be formatted.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getColumn() {
    return column;
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
   * Builder factory method.
   * @return a new Builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }
  
  /**
   * Builder class for TextFormats.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String column;
    private String dateFormat;
    private String dateTimeFormat;
    private String timeFormat;
    private String decimalFormat;
    private String booleanFormat;

    /**
     * Constructor.
     */
    private Builder() {
    }

    /**
     * Set the column to be formatted.
     *
     * @param column The name of the column to be formatted.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder column(String column) {
      this.column = column;
      return this;
    }

    /**
     * Set the Java format to use for date fields.
     *
     * @param dateFormat The format string to use for date fields.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder dateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    /**
     * Set the Java format to use for date/time fields.
     *
     * @param dateTimeFormat The format string to use for date/time fields.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder dateTimeFormat(String dateTimeFormat) {
      this.dateTimeFormat = dateTimeFormat;
      return this;
    }

    /**
     * Set the Java format to use for time fields.
     *
     * @param timeFormat The format string to use for time fields.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder timeFormat(String timeFormat) {
      this.timeFormat = timeFormat;
      return this;
    }

    /**
     * Set the Java format to use for decimal fields.
     *
     * @param decimalFormat The format string to use for decimal fields.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder decimalFormat(String decimalFormat) {
      this.decimalFormat = decimalFormat;
      return this;
    }

    /**
     * Set the Java format to use for boolean fields.
     *
     * @param booleanFormat The format string to use for boolean fields.
     * @return this, so that the builder can be used in a fluent manner.
     */
    public Builder booleanFormat(String booleanFormat) {
      this.booleanFormat = booleanFormat;
      return this;
    }
    
    /**
     * Construct a new ColumnTextFormats object.
     * @return a new ColumnTextFormats object.
     */
    public ColumnTextFormats build() {
      return new ColumnTextFormats(this);
    }
  }

  /**
   * Constructor.
   * @param builder The builder object used to initialise this instance.
   */
  protected ColumnTextFormats(Builder builder) {
    this.column = builder.column;
    this.dateFormat = builder.dateFormat;
    this.dateTimeFormat = builder.dateTimeFormat;
    this.timeFormat = builder.timeFormat;
    this.decimalFormat = builder.decimalFormat;
    this.booleanFormat = builder.booleanFormat;
  }
}
