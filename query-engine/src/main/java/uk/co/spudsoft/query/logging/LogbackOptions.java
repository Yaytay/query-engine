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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.event.Level;

/**
 *
 * @author jtalbut
 */
public class LogbackOptions {
  
  private String configFile;
  
  // This is saying "if running in kubernetes default to using JSON format"
  private boolean jsonFormat = !Strings.isNullOrEmpty(System.getenv("KUBERNETES_SERVICE_HOST"));
  private ImmutableMap<String, Level> level;

  /**
   * Get the location of a standard logback configuration file.
   * This value should only be set if the built-in configuration options are inadequate for your purposes.
   * @return location of a standard logback configuration file.
   */
  public String getConfigFile() {
    return configFile;
  }

  /**
   * The location of a standard logback configuration file.
   * This value should only be set if the built-in configuration options are inadequate for your purposes.
   * @param configFile the location of a standard logback configuration file.
   * @return this, so that the method may be called in a fluent manner.
   */
  public LogbackOptions setConfigFile(String configFile) {
    this.configFile = configFile;
    return this;
  }

  /**
   * If true the logs output to stdout will be in JSON format.
   * This value is ignored if the config file is specified.
   * @return true if the logs output to stdout will be in JSON format.
   */
  public boolean isJsonFormat() {
    return jsonFormat;
  }

  /**
   * When true for the logs output to stdout to be in JSON format.
   * This value is ignored if the config file is specified.
   * @param jsonFormat true for the logs output to stdout to be in JSON format.
   * @return this, so that the method may be called in a fluent manner.
   */
  public LogbackOptions setJsonFormat(boolean jsonFormat) {
    this.jsonFormat = jsonFormat;
    return this;
  }

  /**
   * Get the overridden level of individual {@link org.slf4j.Logger}s.
   * This enables configuration like:
   * logging.level.uk.co.spudsoft.query.main=DEBUG
   * @return the map of loggers to logging level.
   */
  public Map<String, Level> getLevel() {
    return level;
  }

  /**
   * Override the level of individual {@link org.slf4j.Logger}s.
   * This enables configuration like:
   * logging.level.uk.co.spudsoft.query.main=DEBUG
   * @param level the map of loggers to logging level.
   * @return this, so that the method may be called in a fluent manner.
   */
  public LogbackOptions setLevel(Map<String, Level> level) {
    ImmutableMap.Builder<String, Level> builder = ImmutableMap.<String, Level>builder();
    
    for (Iterator<Entry<String, Level>> iter = level.entrySet().iterator(); iter.hasNext();) {
      Entry<String, Level> entry = iter.next();
      if (entry.getKey().contains("_")) {
        String newKey = entry.getKey().replaceAll("_", ".");
        Level value = entry.getValue();
        builder.put(newKey, value);
      } else {
        builder.put(entry);
      }
    }
    this.level = builder.build();
    return this;
  }  
  
}
