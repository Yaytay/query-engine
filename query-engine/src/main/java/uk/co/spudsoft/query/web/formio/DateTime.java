/*
 * Copyright (C) 2023 njt
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;

/**
 *
 * @author njt
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class DateTime extends Component<DateTime> {

  public static enum DatePickerMode {
    day, week, month, year
  }
  
  public static class DatePicker {
    private Boolean showWeeks;
    private Integer startingDay;
    private DatePickerMode minMode;
    private DatePickerMode maxMode;
    private Integer yearRows;
    private Integer yearColumns;
    private LocalDateTime minDate;
    private LocalDateTime maxDate;

    public Boolean getShowWeeks() {
      return showWeeks;
    }

    public void setShowWeeks(Boolean showWeeks) {
      this.showWeeks = showWeeks;
    }

    public Integer getStartingDay() {
      return startingDay;
    }

    public void setStartingDay(Integer startingDay) {
      this.startingDay = startingDay;
    }

    public DatePickerMode getMinMode() {
      return minMode;
    }

    public void setMinMode(DatePickerMode minMode) {
      this.minMode = minMode;
    }

    public DatePickerMode getMaxMode() {
      return maxMode;
    }

    public void setMaxMode(DatePickerMode maxMode) {
      this.maxMode = maxMode;
    }

    public Integer getYearRows() {
      return yearRows;
    }

    public void setYearRows(Integer yearRows) {
      this.yearRows = yearRows;
    }

    public Integer getYearColumns() {
      return yearColumns;
    }

    public void setYearColumns(Integer yearColumns) {
      this.yearColumns = yearColumns;
    }

    public LocalDateTime getMinDate() {
      return minDate;
    }

    public void setMinDate(LocalDateTime minDate) {
      this.minDate = minDate;
    }

    public LocalDateTime getMaxDate() {
      return maxDate;
    }

    public void setMaxDate(LocalDateTime maxDate) {
      this.maxDate = maxDate;
    }

    public DatePicker withShowWeeks(final Boolean value) {
      this.showWeeks = value;
      return this;
    }

    public DatePicker withStartingDay(final Integer value) {
      this.startingDay = value;
      return this;
    }

    public DatePicker withMinMode(final DatePickerMode value) {
      this.minMode = value;
      return this;
    }

    public DatePicker withMaxMode(final DatePickerMode value) {
      this.maxMode = value;
      return this;
    }

    public DatePicker withYearRows(final Integer value) {
      this.yearRows = value;
      return this;
    }

    public DatePicker withYearColumns(final Integer value) {
      this.yearColumns = value;
      return this;
    }

    public DatePicker withMinDate(final LocalDateTime value) {
      this.minDate = value;
      return this;
    }

    public DatePicker withMaxDate(final LocalDateTime value) {
      this.maxDate = value;
      return this;
    }
  }
  
  public static class TimePicker {
    private Integer hourStep;
    private Integer minuteStep;
    private Boolean showMeridian;
    private Boolean readonlyInput;
    private Boolean mousewheel;
    private Boolean arrowkeys;

    public Integer getHourStep() {
      return hourStep;
    }

    public void setHourStep(Integer hourStep) {
      this.hourStep = hourStep;
    }

    public Integer getMinuteStep() {
      return minuteStep;
    }

    public void setMinuteStep(Integer minuteStep) {
      this.minuteStep = minuteStep;
    }

    public Boolean getShowMeridian() {
      return showMeridian;
    }

    public void setShowMeridian(Boolean showMeridian) {
      this.showMeridian = showMeridian;
    }

    public Boolean getReadonlyInput() {
      return readonlyInput;
    }

    public void setReadonlyInput(Boolean readonlyInput) {
      this.readonlyInput = readonlyInput;
    }

    public Boolean getMousewheel() {
      return mousewheel;
    }

    public void setMousewheel(Boolean mousewheel) {
      this.mousewheel = mousewheel;
    }

    public Boolean getArrowkeys() {
      return arrowkeys;
    }

    public void setArrowkeys(Boolean arrowkeys) {
      this.arrowkeys = arrowkeys;
    }

    public TimePicker withHourStep(final Integer value) {
      this.hourStep = value;
      return this;
    }

    public TimePicker withMinuteStep(final Integer value) {
      this.minuteStep = value;
      return this;
    }

    public TimePicker withShowMeridian(final Boolean value) {
      this.showMeridian = value;
      return this;
    }

    public TimePicker withReadonlyInput(final Boolean value) {
      this.readonlyInput = value;
      return this;
    }

    public TimePicker withMousewheel(final Boolean value) {
      this.mousewheel = value;
      return this;
    }

    public TimePicker withArrowkeys(final Boolean value) {
      this.arrowkeys = value;
      return this;
    }
        
  }
  
  private String format;
  private Boolean enableDate;
  private Boolean enableTime;
  private LocalDateTime defaultDate;
  private DatePickerMode datepickerMode;
  private DatePicker datePicker;
  private TimePicker timePicker;
  
  public DateTime() {
    super("datetime");
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public Boolean getEnableDate() {
    return enableDate;
  }

  public void setEnableDate(Boolean enableDate) {
    this.enableDate = enableDate;
  }

  public Boolean getEnableTime() {
    return enableTime;
  }

  public void setEnableTime(Boolean enableTime) {
    this.enableTime = enableTime;
  }

  public LocalDateTime getDefaultDate() {
    return defaultDate;
  }

  public void setDefaultDate(LocalDateTime defaultDate) {
    this.defaultDate = defaultDate;
  }

  public DatePickerMode getDatepickerMode() {
    return datepickerMode;
  }

  public void setDatepickerMode(DatePickerMode datepickerMode) {
    this.datepickerMode = datepickerMode;
  }

  public DatePicker getDatePicker() {
    return datePicker;
  }

  public void setDatePicker(DatePicker datePicker) {
    this.datePicker = datePicker;
  }

  public TimePicker getTimePicker() {
    return timePicker;
  }

  public void setTimePicker(TimePicker timePicker) {
    this.timePicker = timePicker;
  }

  public DateTime withFormat(final String value) {
    this.format = value;
    return this;
  }

  public DateTime withEnableDate(final Boolean value) {
    this.enableDate = value;
    return this;
  }

  public DateTime withEnableTime(final Boolean value) {
    this.enableTime = value;
    return this;
  }

  public DateTime withDefaultDate(final LocalDateTime value) {
    this.defaultDate = value;
    return this;
  }

  public DateTime withDatepickerMode(final DatePickerMode value) {
    this.datepickerMode = value;
    return this;
  }

  public DateTime withDatePicker(final DatePicker value) {
    this.datePicker = value;
    return this;
  }

  public DateTime withTimePicker(final TimePicker value) {
    this.timePicker = value;
    return this;
  }
  
}
