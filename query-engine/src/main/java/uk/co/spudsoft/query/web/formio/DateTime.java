/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web.formio;

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Output a formio DateTime picker.
 *
 * @author jtalbut
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class DateTime extends Component<DateTime> {

  /**
   * Date picker mode.
   */
  public enum DatePickerMode {
    /**
     * The DatePicker is in Day mode.
     */
    day, 
    /**
     * The DatePicker is in Week mode.
     */
    week, 
    /**
     * The DatePicker in in Month mode.
     */
    month, 
    /**
     * The DatePicker is in Year mode.
     */
    year
  }
  
  /**
   * Output a Picker for Dates.
   */
  public static class DatePicker extends AbstractComponent<DatePicker> {
  
    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected DatePicker(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a showWeeks field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withShowWeeks(final Boolean value) throws IOException {
      return with("showWeeks", value);
    }

    /**
     * Output a startingDay field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withStartingDay(final Integer value) throws IOException {
      return with("startingDay", value);
    }

    /**
     * Output a minMode field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withMinMode(final DatePickerMode value) throws IOException {
      return with("minMode", value.name());
    }

    /**
     * Output a maxMode field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withMaxMode(final DatePickerMode value) throws IOException {
      return with("maxMode", value.name());
    }

    /**
     * Output a yearRows field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withYearRows(final Integer value) throws IOException {
      return with("yearRows", value);
    }

    /**
     * Output a yearColumns field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withYearColumns(final Integer value) throws IOException {
      return with("yearColumns", value);
    }

    /**
     * Output a minDate field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withMinDate(final LocalDateTime value) throws IOException {
      return with("minDate", value);
    }

    /**
     * Output a maxDate field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DatePicker withMaxDate(final LocalDateTime value) throws IOException {
      return with("maxDate", value);
    }
  }
  
  /**
   * Output a Picker for Times.
   */
  public static class TimePicker extends AbstractComponent<TimePicker> {
  
    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected TimePicker(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output an hourStep field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withHourStep(final Integer value) throws IOException {
      return with("hourStep", value);
    }

    /**
     * Output a minuteStep field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withMinuteStep(final Integer value) throws IOException {
      return with("minuteStep", value);
    }

    /**
     * Output a showMeridian field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withShowMeridian(final Boolean value) throws IOException {
      return with("showMeridian", value);
    }

    /**
     * Output a readOnlyInput field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withReadonlyInput(final Boolean value) throws IOException {
      return with("readonlyInput", value);
    }

    /**
     * Output a mousewheel field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withMousewheel(final Boolean value) throws IOException {
      return with("mousewheel", value);
    }

    /**
     * Output an arrowkeys field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public TimePicker withArrowkeys(final Boolean value) throws IOException {
      return with("arrowkeys", value);
    }
        
  }
  
  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public DateTime(JsonGenerator generator) throws IOException {
    super(generator, "datetime");
  }

  /**
   * Output a format field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public DateTime withFormat(final String value) throws IOException {
    return with("format", value);
  }

  /**
   * Output an enableDate field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public DateTime withEnableDate(final Boolean value) throws IOException {
    return with("enableDate", value);
  }

  /**
   * Output a enableTime field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public DateTime withEnableTime(final Boolean value) throws IOException {
    return with("enableTime", value);
  }

  /**
   * Output a defaultDate field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public DateTime withDefaultDate(final LocalDateTime value) throws IOException {
    return with("defaultDate", value);
  }

  /**
   * Output a datepickerMode field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public DateTime withDatepickerMode(final DatePickerMode value) throws IOException {
    if (value != null) {
      return with("datepickerMode", value.name());
    } else {
      return this;
    }
  }

  /**
   * Output a datePicker field field.
   * @return a newly created DatePicker object.
   * @throws IOException if the generator fails.
   */
  public DatePicker addDatePicker() throws IOException {
    generator.writeFieldName("datePicker");
    return new DatePicker(generator);
  }

  /**
   * Output a timePicker field field.
   * @return a newly created TimePicker object.
   * @throws IOException if the generator fails.
   */
  public TimePicker addTimePicker() throws IOException {
    generator.writeFieldName("timePicker");
    return new TimePicker(generator);
  }
  
}
