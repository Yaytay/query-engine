/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package uk.co.spudsoft.query.main;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.params4j.impl.Params4JImpl;

/**
 *
 * @author jtalbut
 */
public class Main {
  
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  
  private static final String name = "query-engine";
 
  public static void main(String[] args) {
    Main main = new Main();
    int statusCode = main.innerMain(args);
    if (statusCode >= 0) {
      System.exit(statusCode);
    }
  }
  
  public int testMain(String[] args) {
    int statusCode = innerMain(args);
    if (statusCode >= 0) {
      logger.warn("Should exit with code {}", statusCode);
    }
    return statusCode;
  }
  
  private int innerMain(String[] args) {
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File("/etc/" + name), FileType.Yaml)
            .withSecretsGatherer(new File("/etc/" + name + "/conf.d").toPath(), 0, 0, 0, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(name, false)
            .withSystemPropertiesGatherer(name)
            .withCommandLineArgumentsGatherer(args, name)
            .create();

    Parameters params = p4j.gatherParameters();
    
    if (params.isExitOnRun()) {
      return 1;
    }
    return 0;
  }  
}
