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

package uk.co.spudsoft.query.main;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.vertx.core.AsyncResult;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.web.rest.DesignHandler;

/**
 *
 * @author jtalbut
 */
@OpenAPIDefinition
public class DesignMain extends Main {
  
  private static final Logger logger = LoggerFactory.getLogger(DesignMain.class);
  
  @ExcludeFromJacocoGenerated
  private void mainCompletion(AsyncResult<Integer> result) {
    if (result.succeeded()) {
      int statusCode = result.result();
      if (statusCode > 0) {
        shutdown(statusCode);
      }
    } else {
      logger.error("Failed: ", result.cause());
      shutdown(-1);
    }
  }
  
  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *   
   */
  @ExcludeFromJacocoGenerated
  public static void main(String[] args) {
    DesignMain main = new DesignMain();
    main.innerMain(args).onComplete(main::mainCompletion);
  }
  
  @Override
  protected boolean outputAllErrorMessages() {
    return true;
  }
  
  @Override
  protected void addExtraControllers(Parameters params, List<Object> controllers) {
    controllers.add(new DesignHandler(getVertx(), getDefnLoader(), getDirCache()));
  }

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  protected void prepareBaseConfigPath(File baseConfigFile) {
    if (!baseConfigFile.exists()) {
      baseConfigFile.mkdirs();
    }
    String[] children = baseConfigFile.list();
    if (children != null && children.length == 0) {
      logger.info("Creating sample configs");
      extractSampleFile(baseConfigFile, "samples/demo/FeatureRichExample.yaml");
      extractSampleFile(baseConfigFile, "samples/demo/LookupValues.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/DynamicEndpointPipelineIT.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/JsonToPipelineIT.json");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedJsonToPipelineIT.json.vm");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedYamlToPipelineIT.yaml.vm");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/YamlToPipelineIT.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/permissions.jexl");
      extractSampleFile(baseConfigFile, "samples/sub1/permissions.jexl");
      extractSampleFile(baseConfigFile, "samples/permissions.jexl");
    }
  }
  
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private void extractSampleFile(File baseConfigDir, String path) {
    try {
      File destFile = new File(baseConfigDir, path);
      File destParent = destFile.getParentFile();
      
      if (!destParent.exists()) {
        destParent.mkdirs();
      }
      
      try (InputStream is = getClass().getResourceAsStream("/" + path)) {
        Files.copy(is, destFile.toPath());
      }
      
    } catch (Throwable ex) {
      logger.warn("Failed to copy sample {}: ", ex);
    }
  }
  
}
