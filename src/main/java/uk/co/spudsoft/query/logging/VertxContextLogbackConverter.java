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
package uk.co.spudsoft.query.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import static ch.qos.logback.core.util.OptionHelper.extractDefaultReplacement;

/**
 *
 * @author njt
 */
public class VertxContextLogbackConverter extends ClassicConverter {
  
  private String key;
  private String defaultValue;

  String getKey() {
    return key;
  }

  String getDefaultValue() {
    return defaultValue;
  }
  
  @Override
  public void start() {
    String[] keyInfo = extractDefaultReplacement(getFirstOption());
    key = keyInfo[0];
    defaultValue = keyInfo[1] == null ? "" : keyInfo[1];
    super.start();
  }

  @Override
  public String convert(ILoggingEvent event) {
    if (key == null) {
      return defaultValue;
    }
    Context context = Vertx.currentContext();
    if (context != null) {
      Object value = context.getLocal(key);
      if (value != null) {
        String result = (value instanceof String) ? (String) value : value.toString();
        if (!Strings.isNullOrEmpty(result)) {
          return result;
        }
      }
    }
    return defaultValue;
  }

  @Override
  public void stop() {
    key = null;
    super.stop();
  }  
}
