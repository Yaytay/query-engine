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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
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
  
  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *   
   */
  @ExcludeFromJacocoGenerated
  public static void main(String[] args) {
    Main main = new DesignMain();
    main.innerMain(args, System.out, System.getenv()).onComplete(ar -> main.mainCompletion(ar));
  }
  
  @Override
  protected boolean outputAllErrorMessages() {
    return true;
  }
  
  @Override
  protected void addExtraControllers(Parameters params, List<Object> controllers) {
    logger.info("Running in Design Mode");
    controllers.add(new DesignHandler(getVertx(), getDefnLoader(), getDirCache()));
  }
}
