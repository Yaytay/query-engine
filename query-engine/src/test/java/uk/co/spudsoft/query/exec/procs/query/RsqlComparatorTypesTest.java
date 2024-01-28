/*
 * Copyright (C) 2024 njt
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
package uk.co.spudsoft.query.exec.procs.query;

import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class RsqlComparatorTypesTest {
  
  @Test
  public void testValidateBoolean() {
    RsqlComparatorBoolean instance = new RsqlComparatorBoolean();
    instance.validateType("field", Boolean.FALSE);
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }
  
  @Test
  public void testValidateLong() {
    RsqlComparatorLong instance = new RsqlComparatorLong();
    instance.validateType("field", 6L);
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }
  
  @Test
  public void testValidateDouble() {
    RsqlComparatorDouble instance = new RsqlComparatorDouble();
    instance.validateType("field", 6.9);
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }
  
  @Test
  public void testValidateFloat() {
    RsqlComparatorFloat instance = new RsqlComparatorFloat();
    instance.validateType("field", 1.6f);
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }
  
  @Test
  public void testValidateTime() {
    RsqlComparatorTime instance = new RsqlComparatorTime();
    instance.validateType("field", LocalTime.now());
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", LocalDate.EPOCH);
    });
  }
  
  @Test
  public void testValidateDate() {
    RsqlComparatorDate instance = new RsqlComparatorDate();
    instance.validateType("field", LocalDate.now());
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", 14);
    });
  }
  
  @Test
  public void testValidateString() {
    RsqlComparatorString instance = new RsqlComparatorString();
    instance.validateType("field", "String");
    assertThrows(IllegalStateException.class, () -> {
      instance.validateType("field", 14);
    });
  }

}
