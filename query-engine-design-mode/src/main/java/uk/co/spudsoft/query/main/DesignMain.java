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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.vertx.ext.web.Router;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.query.web.rest.DesignHandler;
import uk.co.spudsoft.jwtvalidatorvertx.vertx.VertxJwksHandler;

/**
 * The DesignMain class serves as an entry point for the application in design mode.
 * It extends the Main class and provides additional configurations and controllers
 * specific to the design environment.
 *
 * @author jtalbut
 */
@OpenAPIDefinition
public class DesignMain extends Main {

  private static final Logger logger = LoggerFactory.getLogger(DesignMain.class);

  /**
   * Default constructor.
   */
  public DesignMain() {
  }

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

  @Override
  protected void addExtraRoutes(Parameters params, Router router) {
    if (params.isEnableForceJwt() && params.getHttpServerOptions().getPort() > 0) {
      VertxJwksHandler handler = new VertxJwksHandler(null, null, "localhost", params.getHttpServerOptions().getPort(), "/testauth", true);      
      Cache<String, AlgorithmAndKeyPair> keyCache = CacheBuilder.newBuilder()
              .expireAfterWrite(Duration.ofDays(1))
              .concurrencyLevel(1)
              .build();
      handler.setKeyCache(keyCache);
      router.route("/testauth/*").handler(handler);
      
      
    }
  }
  
  
}
