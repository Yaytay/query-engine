/*
 * Copyright (C) 2026 jtalbut
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
package uk.co.spudsoft.query.exec.dynamic;

import com.google.common.base.Strings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.stringtemplate.v4.AttributeRenderer;

/**
 * {@link org.stringtemplate.v4.ST StringTemplate} {@link AttributeRenderer} for {@link LocalDateTime} values.
 */
public class StringTemplateLocalDateTimeRenderer implements AttributeRenderer<LocalDateTime> {

  /**
   * Constructor.
   */
  public StringTemplateLocalDateTimeRenderer() {
  }
  
  @Override
  public String toString(LocalDateTime o, String pattern, Locale locale) {
    if (o == null) {
      return "";
    }
    if (Strings.isNullOrEmpty(pattern)) {
      return o.toString();
    }
    DateTimeFormatter fmt = (locale == null) ? DateTimeFormatter.ofPattern(pattern) : DateTimeFormatter.ofPattern(pattern, locale);
    return o.format(fmt);
  }
}
