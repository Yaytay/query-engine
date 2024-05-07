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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.Month;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.web.formio.DateTime.DatePicker;
import uk.co.spudsoft.query.web.formio.DateTime.TimePicker;


/**
 *
 * @author jtalbut
 */
public class DateTimeTest {
  
  private final JsonFactory factory = new JsonFactory();
  
  @Test
  public void testWithFormat() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withFormat(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withFormat("fred");
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"format\":\"fred\"}", baos.toString());    
  }

  @Test
  public void testWithEnableDate() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withEnableDate(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withEnableDate(Boolean.TRUE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"enableDate\":true}", baos.toString());    
  }

  @Test
  public void testWithEnableTime() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withEnableTime(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withEnableTime(Boolean.TRUE);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"enableTime\":true}", baos.toString());    
  }

  @Test
  public void testWithDefaultDate() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withDefaultDate(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withDefaultDate(LocalDateTime.of(2000, Month.JANUARY, 7, 12, 34));
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"defaultDate\":\"2000-01-07T12:34\"}", baos.toString());    
  }

  @Test
  public void testWithDatepickerMode() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withDatepickerMode(null);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\"}", baos.toString());    

    baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        field.withDatepickerMode(DateTime.DatePickerMode.month);
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"datepickerMode\":\"month\"}", baos.toString());    
  }

  @Test
  public void testAddDatePicker() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        try (DatePicker dp = field.addDatePicker()) {
          dp.withMaxMode(DateTime.DatePickerMode.day);
          dp.withMaxDate(LocalDateTime.of(2000, Month.JANUARY, 7, 12, 34));
          dp.withMinMode(DateTime.DatePickerMode.day);
          dp.withMinDate(LocalDateTime.of(1991, Month.JANUARY, 7, 12, 34));
          dp.withShowWeeks(Boolean.FALSE);
          dp.withStartingDay(4);
          dp.withYearColumns(12);
          dp.withYearRows(2);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"datePicker\":{\"maxMode\":\"day\",\"maxDate\":\"2000-01-07T12:34\",\"minMode\":\"day\",\"minDate\":\"1991-01-07T12:34\",\"showWeeks\":false,\"startingDay\":4,\"yearColumns\":12,\"yearRows\":2}}", baos.toString());    
  }

  @Test
  public void testAddTimePicker() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JsonGenerator generator = factory.createGenerator(baos)) {
      try (DateTime field = new DateTime(generator)) {
        try (TimePicker tp = field.addTimePicker()) {
          tp.withArrowkeys(Boolean.FALSE);
          tp.withHourStep(2);
          tp.withMinuteStep(7);
          tp.withMousewheel(Boolean.FALSE);
          tp.withReadonlyInput(Boolean.FALSE);
          tp.withShowMeridian(Boolean.FALSE);
        }
      }
    }
    baos.close();
    assertEquals("{\"type\":\"datetime\",\"timePicker\":{\"arrowkeys\":false,\"hourStep\":2,\"minuteStep\":7,\"mousewheel\":false,\"readonlyInput\":false,\"showMeridian\":false}}", baos.toString());    
  }
  
}
