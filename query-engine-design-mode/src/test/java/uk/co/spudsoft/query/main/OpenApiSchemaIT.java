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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import io.vertx.junit5.Timeout;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class OpenApiSchemaIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File paramsDir = new File("target/query-engine/samples-mainqueryit");
    Main.prepareBaseConfigPath(paramsDir, null);
    postgres.prepareTestDatabase(vertx)
            .onComplete(testContext.succeedingThenComplete())
            ;
    new File("target/classes/samples/sub1/sub3").mkdirs();
  }
  
  @Test
  @Timeout(value = 2400, timeUnit = TimeUnit.SECONDS)
  public void testQuery() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.user.username=" + postgres.getUser()
      , "--persistence.datasource.user.password=" + postgres.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrementMs=500"
      , "--baseConfigPath=target/query-engine/samples-mainqueryit"
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--zipkin.baseUrl=http://localhost/wontwork"
    }, stdout, Collections.emptyMap());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    JsonObject jo = new JsonObject(body);
    logger.info("OpenAPI: {}", jo);
    JsonObject schemas = jo.getJsonObject("components").getJsonObject("schemas");
    List<String> validationFailures = new ArrayList<>();
    validateRefs(validationFailures, body);
    for (String current : schemas.fieldNames()) {
      JsonObject schema = schemas.getJsonObject(current);
      // logger.debug("{}: {}", current, describe(schema));
      String validationMessage = validate(current, schema, true);
      if (validationMessage != null) {
        validationFailures.add(validationMessage + " " + schema.encode());
      }
      JsonObject props = schema.getJsonObject("properties");
      if (schema.containsKey("allOf")) {
        props = schema.getJsonArray("allOf").getJsonObject(1).getJsonObject("properties");
      }
      if (props != null) {
        for (Iterator<Entry<String,Object>> iter = props.iterator(); iter.hasNext(); ) {
          Entry<String,Object> propSchemaEntry = iter.next();
          String qualName = current + "." + propSchemaEntry.getKey();
          if (propSchemaEntry.getValue() instanceof JsonObject propSchema) {
            // logger.debug("{}: {}", qualName, describe(propSchema)); 

            validationMessage = validate(qualName, propSchema, false);
            if (validationMessage != null) {
              validationFailures.add(validationMessage + " " + propSchema.encode());
            }
          }
        }
      }      
    }
    
    for (String validationFailure : validationFailures) {
      logger.warn(validationFailure);
    }
    assertTrue(validationFailures.isEmpty(), "At least one (" + validationFailures.size() + ") validation failed");
    
    Set<String> refProps = new HashSet<>();
    collateProps(refProps, body, "$ref");
    logger.debug("Properties on $ref structures are: {}", refProps.stream().sorted().toList());
    
    Set<String> typeProps = new HashSet<>();
    collateProps(typeProps, body, "type");
    logger.debug("Properties on type structures are: {}", typeProps.stream().sorted().toList());
    
        
    main.shutdown();
  }

  private String describe(JsonObject schema) {
    String type = schema.getString("type");
    if (type == null) {
      type = schema.getString("$ref");
      if (type != null) {
        type = type.replaceAll("#/components/schemas/", "");
      }
    }
    if (schema.getValue("allOf") != null) {
      JsonArray allOf = schema.getJsonArray("allOf");
      String parent = null;
      for (Iterator<Object> iter = allOf.iterator(); iter.hasNext(); ) {
        Object oItem = iter.next();
        if (oItem instanceof JsonObject item) {
          if (item.containsKey("$ref")) {
            String ref = item.getString("$ref");
            parent = ref.replaceAll("#/components/schemas/", "");
          } else {
            type = item.getString("type");
          }
        }
      }
      type = type + " subclass of " + parent;
    } else if ("array".equals(type)) {
      JsonObject items = schema.getJsonObject("items");
      String ref = items.getString("$ref");
      if (!Strings.isNullOrEmpty(ref)) {
        type = type + " of " + ref.replaceAll("#/components/schemas/", "");
      } else {
        String itemType = items.getString("type");
        if ("string".equals(itemType)) {
          if (items.containsKey("maxLength")) {
            itemType = itemType + "[" + items.getString("maxLength")  + "]";
          } else {
            itemType = "clob";
          }
        }
        type = type + " of " + itemType;
      }
    } else if ("object".equals(type)) {
      if (schema.containsKey("additionalProperties")) {
        JsonObject addProp = schema.getJsonObject("additionalProperties");
        String ref = addProp.getString("$ref");
        if (ref != null) {
          ref = ref.replaceAll("#/components/schemas/", "");
          type = "map to " + ref + " objects";
        }
      }
    } else if ("string".equals(type)) {
      if (schema.containsKey("maxLength")) {
        type = type + "[" + schema.getString("maxLength")  + "]";
      } else {
        type = "clob";
      }
    }
    if (schema.containsKey("enum")) {
      JsonArray options = schema.getJsonArray("enum");
      type = type + " (" + options.size() + " possible values)";
    }
    if (type == null) {
      type = "Unknown";
    }
    if (schema.containsKey("description")) {
      type = type + " (has description)";
    }
    
    return type;
  }
  
  private void collateProps(Set<String> props, String jsonSchema, String key) throws Exception {
    ObjectNode root = (ObjectNode) DatabindCodec.mapper().readTree(jsonSchema);
    ObjectNode schemas = (ObjectNode) root.get("components").get("schemas");
    collateProps(props, schemas, key);
  }

  private void collateProps(Set<String> props, ObjectNode schemas, String key) {
    List<JsonNode> parents = schemas.findParents(key);
    for (JsonNode parent : parents) {
      JsonNode keyValue = parent.get(key);
      // Skip anything that might have used the key for something else (mainly because we have classes with "type" fields
      if (keyValue.isTextual()) { 
        for (Iterator<Entry<String, JsonNode>> iter = parent.fields(); iter.hasNext();){
          Entry<String, JsonNode> entry = iter.next();
          String field = entry.getKey();
          props.add(field);
          if (entry.getValue() instanceof ObjectNode fieldObject) {
            collateProps(props, fieldObject, key);
          }
        }
      }
    }
  }
  
  private void validateRefs(List<String> messages, String jsonSchema) {
    try {
      ObjectNode root = (ObjectNode) DatabindCodec.mapper().readTree(jsonSchema);
      ObjectNode schemas = (ObjectNode) root.get("components").get("schemas");
      List<JsonNode> parents = schemas.findParents("$ref");
      for (JsonNode parent : parents) {
        String ref = parent.get("$ref").textValue();
        String cls = ref.replaceAll("#/components/schemas/", "");
        if (!schemas.has(cls)) {
          messages.add("There is a reference to " + ref + " but no " + cls + " exists in schemas");
        }
      }
    } catch (Throwable ex) {
      messages.add(ex.getMessage());
    }
  }
  
  private String validate(String name, JsonObject schema, boolean isTopLevelSchema) {
    if (!schema.containsKey("description")) {
      return name + " has no description";
    }
    
    String type = schema.getString("type");
    if ("object".equals(type) && schema.containsKey("additionalProperties")) {
      return name + " is a map (object with additionalProperties), please change to a List";
    }
    if (isTopLevelSchema) {
      // Must either have a type, or have an allOf with a ref and then a type
      // In both cases the type must be "object"
       if (Strings.isNullOrEmpty(type)) {
         JsonArray allOf = schema.getJsonArray("allOf");
         if (allOf == null || allOf.isEmpty()) {
           return name + " has no type or allOf field";
         } else {
           if (allOf.size() != 2) {
             return name + " has allOf array with " + allOf.size() + " values";
           }
           if (!allOf.getJsonObject(0).containsKey("$ref")) {
             return name + " has allOf array with first element that does not have a \"$ref\" field";
           }
           type = allOf.getJsonObject(1).getString("type");
           if (Strings.isNullOrEmpty(type)) {
             return name + " has allOf array with select element that does not have a \"type\" field";
           }
         }
       }
       if (!"object".equals(type)) {
         return name + " is a top level schema type that is not an object";
       }
    } else {
      // Properties must all have a type or a $ref
      if (Strings.isNullOrEmpty(type) && !schema.containsKey("$ref")) {
        return name + " has no \"type\" or \"$ref\" field";
      }
      // Additionally, array properties must have an item ref
      if ("array".equals(type)) {
        JsonObject  items = schema.getJsonObject("items");
        if (items == null) {
          return name + " is an array but has no \"items\" field";
        }
        if (!items.containsKey("$ref") && !items.containsKey("type")) {
          return name + " is an array and has an \"items\" field, but that \"items\" object has no \"$ref\" or \"type\" field";
        }
      }
    }
    if (schema.containsKey("enum")) {
      if (!"string".equals(type)) {
        return name + " is an enum that isn't a string";
      }
    } else if ("string".equals(type)
            && (
            !"date-time".equals(schema.getString("format"))
            )
      ) {
      if (!schema.containsKey("maxLength")) {
        return name + " is a string with no maxiumum length";
      }
    }
    
    return null;
  }
}
