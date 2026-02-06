/*
 * Copyright (C) 2023 jtalbut
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

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.ws.rs.core.Application;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.web.rest.DocHandler;

/**
 *
 * @author jtalbut
 */
@OpenAPIDefinition
public class OpenApiModelTest extends Application {

  private static final Logger logger = LoggerFactory.getLogger(OpenApiModelTest.class);

  @Test
  public void testPipeline() throws Throwable {

    ModelConverters.getInstance(true).addConverter(new OpenApiModelConverter());

    OpenAPIConfiguration openApiConfig = Main.createOpenapiConfiguration(Arrays.asList(new OpenApiTestController()), this, "OpenApiModelTest#testPipeline", true);

    JaxrsOpenApiContextBuilder<?> oacb = new JaxrsOpenApiContextBuilder<>()
            .application(this);
    oacb.setOpenApiConfiguration(openApiConfig);
    OpenApiContext ctx = oacb.buildContext(true);

    OpenAPIConfiguration config = ctx.getOpenApiConfiguration();
    if (config == null) {
      config = openApiConfig;
    }

    OpenAPI oas = ctx.read();

    if (config.isOpenAPI31()) {
      logger.error("Pipeline OpenAPI 3.1: {}", Json31.pretty(oas));
    } else {
      logger.error("Pipeline OpenAPI 3.0: {}", Json.pretty(oas));
    }
    
    assertNotNull(oas);
  }
  
  @Test
  public void testDocHandler() throws Throwable {

    ModelConverters.getInstance(true).addConverter(new OpenApiModelConverter());

    OpenAPIConfiguration openApiConfig = Main.createOpenapiConfiguration(Arrays.asList(new DocHandler(null, true, false)), this, "OpenApiModelTest#testDocHandler", true);

    JaxrsOpenApiContextBuilder<?> oacb = new JaxrsOpenApiContextBuilder<>()
            .application(this);
    oacb.setOpenApiConfiguration(openApiConfig);
    OpenApiContext ctx = oacb.buildContext(true);

    OpenAPIConfiguration config = ctx.getOpenApiConfiguration();
    if (config == null) {
      config = openApiConfig;
    }

    OpenAPI oas = ctx.read();

    if (config.isOpenAPI31()) {
      logger.error("DocHandler OpenAPI 3.1: {}", Json31.pretty(oas));
    } else {
      logger.error("DocHandler OpenAPI 3.0: {}", Json.pretty(oas));
    }
    
    assertNotNull(oas);
  }
}
