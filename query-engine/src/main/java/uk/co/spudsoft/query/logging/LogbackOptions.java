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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.event.Level;

/**
 * Configuration data for logging.
 * <P>
 * Logging uses <a href="https://logback.qos.ch/">logback</a>.
 * 
 * @author jtalbut
 */
public class LogbackOptions {
  
  private String configFile;
  
  // This is saying "if running in kubernetes default to using JSON format" - it would be better if it could also identify deployment to Docker Swarm, but it can't.
  private boolean jsonFormat = !Strings.isNullOrEmpty(System.getenv("KUBERNETES_SERVICE_HOST"));
  private ImmutableMap<String, Level> level;

  /**
   * Constructor.
   */
  public LogbackOptions() {
  }

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
   * <P>
   * This value should only be set if the built-in configuration options are inadequate for your purposes.
   * <P>
   * By default logging is all to stdout and is at INFO level for all loggers.
   * The format used is 
   * <pre>"%date{yyyy-MM-dd HH:mm:ss.SSS, UTC} [%thread] %-5level %logger{36} %X{traceId:-#}:%X{spanId:-#} %X{source:-#} - %msg%n"</pre> 
   * which is a fairly standard format with the addition of fields for the trace and source details.
   * @param configFile the location of a standard logback configuration file.
   * @return this, so that the method may be called in a fluent manner.
   */
  public LogbackOptions setConfigFile(String configFile) {
    this.configFile = configFile;
    return this;
  }

  /**
   * If true the logs output to stdout will be in JSON format.
   * <P>
   * This value is ignored if the config file is specified.
   * <P>
   * If this value is set to true the logback JsonEncoder will be used with the following options:
   * <P>
   * <pre>
   *encoder.setWithArguments(false);
   *encoder.setWithMessage(false);
   *encoder.setWithFormattedMessage(true);
   *encoder.setWithSequenceNumber(false);
   *encoder.setWithContext(false);
   * </pre>
   * @return true if the logs output to stdout will be in JSON format.
   */
  public boolean isJsonFormat() {
    return jsonFormat;
  }

  /**
   * If true the logs output to stdout will be in JSON format.
   * <P>
   * This value is ignored if the config file is specified.
   * <P>
   * When running in a production environment with any kind of log collation (e.g. most of the built in docker logging drivers) 
   * it is advisable to use JSON output to ensure that multiline log records (such as stack traces) come across as a single 
   * record in the collated system.
   * <P>
   * If this value is set to true the logback JsonEncoder will be used with the following options:
   * <P>
   * <pre>
   *encoder.setWithArguments(false);
   *encoder.setWithMessage(false);
   *encoder.setWithFormattedMessage(true);
   *encoder.setWithSequenceNumber(false);
   *encoder.setWithContext(false);
   * </pre>
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
   * <P>
   * This enables configuration like (system property):
   * <PRE>
   *-Dlogging.level.uk.co.spudsoft.query.main=DEBUG
   * </PRE>
   * Note that log levels can also be set dynamically using the management endpoint for loggers.
   * 
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
  
  /**
   * Validate the provided parameters.
   * 
   * @param fieldName The name of the parent parameter, to be used in exception messages.
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public void validate(String fieldName) throws IllegalArgumentException {
    if (!Strings.isNullOrEmpty(configFile)) {
      if (!new File(configFile).exists()) {
        throw new IllegalArgumentException("The file specified as " + fieldName + ".configFile (" + configFile + ") does not exist");
      }
    }
  }

}
