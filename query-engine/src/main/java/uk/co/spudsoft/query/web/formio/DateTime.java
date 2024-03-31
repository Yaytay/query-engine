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

  public enum DatePickerMode {
    day, week, month, year
  }
  
  public static class DatePicker extends AbstractComponent<DatePicker> {
  
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected DatePicker(JsonGenerator generator) throws IOException {
      super(generator);
    }

    public DatePicker withShowWeeks(final Boolean value) throws IOException {
      return with("showWeeks", value);
    }

    public DatePicker withStartingDay(final Integer value) throws IOException {
      return with("startingDay", value);
    }

    public DatePicker withMinMode(final DatePickerMode value) throws IOException {
      return with("minMode", value.name());
    }

    public DatePicker withMaxMode(final DatePickerMode value) throws IOException {
      return with("maxMode", value.name());
    }

    public DatePicker withYearRows(final Integer value) throws IOException {
      return with("yearRows", value);
    }

    public DatePicker withYearColumns(final Integer value) throws IOException {
      return with("yearColumns", value);
    }

    public DatePicker withMinDate(final LocalDateTime value) throws IOException {
      return with("minDate", value);
    }

    public DatePicker withMaxDate(final LocalDateTime value) throws IOException {
      return with("maxDate", value);
    }
  }
  
  public static class TimePicker extends AbstractComponent<TimePicker> {
  
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected TimePicker(JsonGenerator generator) throws IOException {
      super(generator);
    }

    public TimePicker withHourStep(final Integer value) throws IOException {
      return with("hourStep", value);
    }

    public TimePicker withMinuteStep(final Integer value) throws IOException {
      return with("minuteStep", value);
    }

    public TimePicker withShowMeridian(final Boolean value) throws IOException {
      return with("showMeridian", value);
    }

    public TimePicker withReadonlyInput(final Boolean value) throws IOException {
      return with("readonlyInput", value);
    }

    public TimePicker withMousewheel(final Boolean value) throws IOException {
      return with("mousewheel", value);
    }

    public TimePicker withArrowkeys(final Boolean value) throws IOException {
      return with("arrowkeys", value);
    }
        
  }
  
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public DateTime(JsonGenerator generator) throws IOException {
    super(generator, "datetime");
  }

  public DateTime withFormat(final String value) throws IOException {
    return with("format", value);
  }

  public DateTime withEnableDate(final Boolean value) throws IOException {
    return with("enableDate", value);
  }

  public DateTime withEnableTime(final Boolean value) throws IOException {
    return with("enableTime", value);
  }

  public DateTime withDefaultDate(final LocalDateTime value) throws IOException {
    return with("defaultDate", value);
  }

  public DateTime withDatepickerMode(final DatePickerMode value) throws IOException {
    if (value != null) {
      return with("datepickerMode", value.name());
    } else {
      return this;
    }
  }

  public DatePicker addDatePicker() throws IOException {
    generator.writeFieldName("datePicker");
    return new DatePicker(generator);
  }

  public TimePicker addTimePicker() throws IOException {
    generator.writeFieldName("timePicker");
    return new TimePicker(generator);
  }
  
}
