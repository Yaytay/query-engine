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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 *
 * @author jtalbut
 */
public class LoggingConfiguration {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

  public static void configureLogback(LoggerContext loggerContext, LogbackOptions options) {
    if (options == null) {
      // No configuration, so just leave as default
      return;
    }

    loggerContext.reset();
    
    if (!Strings.isNullOrEmpty(options.getConfigFile())) {
      if (configureFromFile(loggerContext, options.getConfigFile())) {
        if (options.getLevel() != null) {
          overrideLevels(options.getLevel());
        }
        return;
      }
    }

    Map<String, String> ruleRegistry = getRuleRegistry(loggerContext);
    registerConverters(ruleRegistry);

    ConsoleAppender<ILoggingEvent> appender;
    if (options.isJsonFormat()) {
      appender = configureForJson(loggerContext);
    } else {
      appender = configureForText(loggerContext);
    }
    appender.start();
    
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(appender);
    rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);

    Logger gtiLogger = loggerContext.getLogger("uk.co.spudsoft");
    gtiLogger.addAppender(appender);
    gtiLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
    gtiLogger.setAdditive(false);

    if (options.getLevel() != null) {
      overrideLevels(options.getLevel());
    }

  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> getRuleRegistry(LoggerContext loggerContext) {
    Map<String, String> ruleRegistry = (Map<String, String>) loggerContext.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
    if (ruleRegistry == null) {
      ruleRegistry = new HashMap<>();
      loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
    }
    return ruleRegistry;
  }

  @SuppressFBWarnings(value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE", justification = "No other way to report the problem to user, the user should be a sysadmin")
  private static boolean configureFromFile(LoggerContext loggerContext, String filename) {
    JoranConfigurator configurator = new JoranConfigurator();
    try (InputStream configStream = openConfigFile(filename)) {
      configurator.setContext(loggerContext);
      configurator.doConfigure(configStream); // loads logback file
      return true;
    } catch (IOException | JoranException ex) {
      logger.error("Failed to configure logging from file: ", ex);
      return false;
    }
  }

  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The file specified as logging.configFile must be trusted")
  private static FileInputStream openConfigFile(String filename) throws FileNotFoundException {
    return new FileInputStream(filename);
  }

  private static void registerConverters(Map<String, String> ruleRegistry) {
    ruleRegistry.put("vz", VertxZipkinLogbackConverter.class.getName());
    ruleRegistry.put("vc", VertxContextLogbackConverter.class.getName());
  }

  private static ConsoleAppender<ILoggingEvent> createConsoleAppender(LoggerContext lc) {
    ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
    ca.setContext(lc);
    ca.setName("STDOUPUT");
    ca.setImmediateFlush(true);
    return ca;
  }

  private static ConsoleAppender<ILoggingEvent> configureForJson(LoggerContext loggerContext) {
    ConsoleAppender<ILoggingEvent> ca = createConsoleAppender(loggerContext);

    LogstashEncoder encoder = new LogstashEncoder();
    encoder.setContext(loggerContext);
    encoder.setIncludeMdc(true);
    encoder.setIncludeCallerData(true);
    encoder.setIncludeContext(true);
    LogstashVertxContextJsonProvider<ILoggingEvent> provider = new LogstashVertxContextJsonProvider<>();
    provider.setFieldName("context");
    encoder.addProvider(provider);
    encoder.start();

    ca.setEncoder(encoder);

    return ca;
  }

  private static ConsoleAppender<ILoggingEvent> configureForText(LoggerContext loggerContext) {
    ConsoleAppender<ILoggingEvent> ca = createConsoleAppender(loggerContext);

    PatternLayout layout = new PatternLayout();
    layout.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS, UTC} [%thread] %-5level %logger{36} %vz{all:-#} %vc{uk.co.spudsoft.query.exec.source.name:-#} - %msg%n");

    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    encoder.setContext(loggerContext);

    layout.setContext(loggerContext);
    layout.start();
    encoder.setLayout(layout);
    encoder.start();

    ca.setEncoder(encoder);

    return ca;
  }

  private static void overrideLevels(Map<String, Level> levels) {
    for (Entry<String, Level> entry : levels.entrySet()) {
      String loggerName = entry.getKey();
      ch.qos.logback.classic.Logger lg = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
      ch.qos.logback.classic.Level lvl = getLogbackLogLevel(entry.getValue());
      logger.info("Changing {} to {} due to environment variable", lg, lvl);
      lg.setLevel(lvl);
    }
  }

  private static ch.qos.logback.classic.Level getLogbackLogLevel(org.slf4j.event.Level slf4jLevel) {
    return switch (slf4jLevel) {
      case TRACE -> ch.qos.logback.classic.Level.TRACE;
      case DEBUG -> ch.qos.logback.classic.Level.DEBUG;
      case INFO -> ch.qos.logback.classic.Level.INFO;
      case WARN -> ch.qos.logback.classic.Level.WARN;
      case ERROR -> ch.qos.logback.classic.Level.ERROR;
    };
  }

}
