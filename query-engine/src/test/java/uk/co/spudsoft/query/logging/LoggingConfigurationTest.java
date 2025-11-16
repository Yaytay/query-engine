
package uk.co.spudsoft.query.logging;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 *
 * @author jtalbut
 */
@TestMethodOrder(MethodName.class)
public class LoggingConfigurationTest {
  
  private static final Logger logger = LoggerFactory.getLogger(LoggingConfigurationTest.class);
  
  @Test
  public void test1Default() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LoggingConfiguration.configureLogback(loggerContext, null, null);
    logger.info("Hello by default");
  }
  
  @Test
  public void test2Json() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LogbackOptions options = new LogbackOptions().setJsonFormat(true);
    LoggingConfiguration.configureLogback(loggerContext, options, null);
    logger.info("Hello Json");
    
    options = new LogbackOptions().setJsonFormat(false);
    LoggingConfiguration.configureLogback(loggerContext, options, null);
    logger.info("Hello Text");
  }
  
  @Test
  public void test3ConfigFile() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LogbackOptions options = new LogbackOptions()
            .setConfigFile("target/test-classes/logback-test.xml")
            .setLevel(
                    ImmutableMap.<String, Level>builder()
                            .put("uk.co.spudsoft.query.exec", Level.WARN)
                            .put("uk.co.spudsoft.query.exec.procs", Level.DEBUG)
                            .put("uk.co.spudsoft.query.exec.sources", Level.INFO)
                            .put("uk.co.spudsoft.query.json", Level.ERROR)
                            .build()
            )
            ;
    LoggingConfiguration.configureLogback(loggerContext, options, null);
    logger.info("Hello with config file");
  }
  
  @Test
  public void test4Reset() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    LogbackOptions options = new LogbackOptions()
            .setConfigFile("target/test-classes/logback-test.xml")
            ;
    LoggingConfiguration.configureLogback(loggerContext, options, null);
    logger.info("Hello after reset");
  }
}
