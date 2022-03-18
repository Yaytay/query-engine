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

/**
 *
 * @author jtalbut
 */
public class Main {
  
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  
  private static final String NAME = "query-engine";
 
  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *   
   */
  public static void main(String[] args) {
    Main main = new Main();
    int statusCode = main.innerMain(args);
    if (statusCode > 0) {
      System.exit(statusCode);
    }
  }
  
  /**
   * Method to allow test code to call main with no risk of System.exit being called.
   * @param args Command line arguments.
   * @return The status code that would have been returned if this was a real command line execution.
   */
  public int testMain(String[] args) {
    int statusCode = innerMain(args);
    if (statusCode > 0) {
      logger.warn("Should exit with code {}", statusCode);
    }
    return statusCode;
  }
  
  private int innerMain(String[] args) {
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File("/etc/" + NAME), FileType.Yaml)
            .withSecretsGatherer(new File("/etc/" + NAME + "/conf.d").toPath(), 0, 0, 0, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(NAME, false)
            .withSystemPropertiesGatherer(NAME)
            .withCommandLineArgumentsGatherer(args, NAME)
            .create();

    Parameters params = p4j.gatherParameters();
    
    if (params.isExitOnRun()) {
      return 1;
    }
    return 0;
  }  
}
