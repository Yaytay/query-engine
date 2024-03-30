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
package uk.co.spudsoft.query.web.rest;

import io.opentelemetry.api.GlobalOpenTelemetry;
import uk.co.spudsoft.query.main.*;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * A set of tests that do not actually do any querying.
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class DesignHandlerIT {

  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(DesignHandlerIT.class);

  static void deleteFolder(File file) {
    for (File subFile : file.listFiles()) {
      if (subFile.isDirectory()) {
        deleteFolder(subFile);
      } else {
        subFile.delete();
      }
    }
    file.delete();
  }

  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File sourceDir = new File("target/query-engine/samples-designhandlerit/sub1/newfolder");
    if (sourceDir.exists()) {
      deleteFolder(sourceDir);
    }
    sourceDir = new File("target/query-engine/samples-designhandlerit/sub1/newdir");
    if (sourceDir.exists()) {
      deleteFolder(sourceDir);
    }

    postgres.prepareTestDatabase(vertx).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testDesignHandler() throws Exception {
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--audit.datasource.url=" + postgres.getJdbcUrl()
      , "--audit.datasource.adminUser.username=" + postgres.getUser()
      , "--audit.datasource.adminUser.password=" + postgres.getPassword()
      , "--audit.datasource.schema=public"
      , "--baseConfigPath=target/query-engine/samples-designhandlerit"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=true"
    }, stdout);

    RestAssured.port = main.getPort();

    given().log().all()
            .get("/openapi.yaml")
            .then().log().all()
            .statusCode(200)
            ;

    given().log().all()
            .get("/api/design/enabled")
            .then().log().all()
            .statusCode(200)
            .body(equalTo("true"));
    
    String startingFiles = given().log().all()
            .get("/api/design/all")
            .then().log().all()
            .statusCode(200)
            .extract().body().asString();

    given().log().all()
            .get("/api/design/pipeline/sub1/sub2/YamlToPipelineIT.yaml")
            .then().log().all()
            .statusCode(200)
            .body(startsWith("{\"source\":{\"type\":\"SQL\",\"endpointTemplate\":"));            

    given().log().all()
            .get("/api/design/pipeline/sub1/sub2/JsonToPipelineIT.json")
            .then().log().all()
            .statusCode(200);

    given().log().all()
            .get("/api/design/file/sub1/sub2/YamlToPipelineIT.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/yaml"))
            .body(startsWith("description:"));

    // Can get a yaml file as json
    String json = given().accept(ContentType.JSON).log().all()
            .get("/api/design/file/sub1/sub2/YamlToPipelineIT.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body(startsWith("{\""))
            .extract().body().asString();
    
    // Can get a json file as yaml
    given().accept("application/yaml").log().all()
            .get("/api/design/file/sub1/sub2/JsonToPipelineIT.json")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/yaml"))
            .body(startsWith("---\ntitle: \""))
            .extract().body().asString();
    
    // And can put a yaml file as json
    given().contentType(ContentType.JSON).log().all()
            .body(json)
            .put("/api/design/file/sub1/sub2/YamlToPipelineIT.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body(startsWith("{\""))
            ;

    // Can validate a JSON pipeline
    given().contentType(ContentType.JSON).log().all()
            .body(json)
            .post("/api/design/validate")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(equalTo("The pipeline is valid"))
            .extract().body().asString();
    
    // Content for validation must be valid
    given().contentType(ContentType.JSON).log().all()
            .body("not really json")
            .post("/api/design/validate")
            .then().log().all()
            .statusCode(400)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(startsWith("The JSON body cannot be parsed as a Pipeline: Unrecognized token 'not'"))
            .extract().body().asString();

    // Can also fail to validate a JSON pipeline
    json = json.replaceAll("sqlserver://localhost:<args.port>/test", "");
    given().contentType(ContentType.JSON).log().all()
            .body(json)
            .post("/api/design/validate")
            .then().log().all()
            .statusCode(400)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(startsWith("The Pipeline is not valid"))
            .extract().body().asString();

    // Can also fail to validate a YAML pipeline
    given().contentType("application/yaml").log().all()
            .body("source:\nbox:\n".getBytes())
            .post("/api/design/validate")
            .then().log().all()
            .statusCode(400)
            .contentType(startsWith("text/plain"))
            .body(startsWith("The Pipeline is not valid"))
            .extract().body().asString();

    // Can get a json template file as specific type
    given().accept("application/json+velocity").log().all()
            .get("/api/design/file/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json+velocity"))
            .body(startsWith("{\r\n  \"arguments\": ["));
    
    // Can get a json template file as correct type even if asked for any
    given().accept(ContentType.ANY).log().all()
            .get("/api/design/file/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json+velocity"))
            .body(startsWith("{\r\n  \"arguments\": ["));
    
    // Can get a yaml template file as plain text even if asked for any
    given().accept(ContentType.ANY).log().all()
            .get("/api/design/file/sub1/sub2/TemplatedYamlToPipelineIT.yaml.vm")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/yaml+velocity"))
            .body(startsWith("arguments:\r\n  - name: key\r\n"));
    
    // Request for a file that does not exist should 404
    given().accept(ContentType.ANY).log().all()
            .get("/api/design/file/sub4/sub5/Nonexistent.yml")
            .then().log().all()
            .statusCode(404)
            .contentType(startsWith("text/plain"))
            // Outputs the full error message because this is in "outputAllErrorMessages" mode
            .body(startsWith("Unable to read file at path 'target" + File.separator + "query-engine" + File.separator + "samples-designhandlerit" + File.separator + "sub4" + File.separator + "sub5" + File.separator + "Nonexistent.yml'"));
    
    // Request for a valid file using .. should fail
    given().accept(ContentType.ANY).log().all()
            .get("/api/design/file/sub1/../sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then().log().all()
            .statusCode(400)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(startsWith("Path may not contain .. (from ServiceException"));
    
    // Request for a file with a leading / should fail
    given().accept(ContentType.ANY).log().all()
            .urlEncodingEnabled(false)
            .get("/api/design/file/%2Ftemp")
            .then().log().all()
            .statusCode(400)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(startsWith("Path may not start with / (from ServiceException"));
    
    // Request for a file with a \ should fail
    given().accept(ContentType.ANY).log().all()
            .urlEncodingEnabled(false)
            .get("/api/design/file/sub1%5Csub2/TemplatedJsonToPipelineIT.json.vm")
            .then().log().all()
            .statusCode(400)
            .contentType(equalTo("text/plain;charset=UTF-8"))
            .body(startsWith("Path may not contain banned characters (must not match /\\\"|\\||<|>|\\:|\\?|\\*|\\\\|\\p{C}/) (from ServiceException"));
    
    given().log().all()
            .get("/api/design/file/sub1/sub2/permissions.jexl")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/jexl"));

    // Can create new directory
    given().log().all()
            .contentType("inode/directory")
            .put("/api/design/file/sub1/newfolder")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].name", equalTo("newfolder"));

    // Can create new subdirectory
    given().log().all()
            .contentType("inode/directory")
            .put("/api/design/file/sub1/newfolder/subfolder")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].name", equalTo("newfolder"));

    // Cannot put directory with extension
    given().log().all()
            .contentType("inode/directory")
            .put("/api/design/file/sub1/new.folder")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal folder name (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot put file without extension
    given().log().all()
            .contentType("application/yaml")
            .body(Main.class.getResourceAsStream("/samples/demo/LookupValues.yaml"))
            .put("/api/design/file/sub1/newfolder/newyaml")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal file name (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot put file with wrong extension
    given().log().all()
            .contentType("application/yaml")
            .body(this.getClass().getResourceAsStream("/samples/demo/LookupValues.yaml"))
            .put("/api/design/file/sub1/newfolder/new.json")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal file name; extension does not match content-type (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Can put correct file
    given().log().all()
            .contentType("application/yaml")
            .body(this.getClass().getResourceAsStream("/samples/demo/LookupValues.yaml"))
            .put("/api/design/file/sub1/newfolder/new.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].children[1].name", equalTo("new.yaml"));

    // Can put another correct file
    given().log().all()
            .contentType("application/yaml")
            .body(this.getClass().getResourceAsStream("/samples/demo/LookupValues.yaml"))
            .put("/api/design/file/sub1/newfolder/new2.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].children[1].name", equalTo("new.yaml"));

    // Can rename file
    given().log().all()
            .queryParam("name", "bob.yaml")
            .post("/api/design/rename/sub1/newfolder/new.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].children[1].name", equalTo("bob.yaml"));

    // Cannot rename file to name that already exists
    given().log().all()
            .queryParam("name", "new2.yaml")
            .post("/api/design/rename/sub1/newfolder/bob.yaml")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Destination file already exists (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot rename file to no extension
    given().log().all()
            .queryParam("name", "file")
            .post("/api/design/rename/sub1/newfolder/bob.yaml")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal file name (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot rename file to new extension
    given().log().all()
            .queryParam("name", "file.json")
            .post("/api/design/rename/sub1/newfolder/bob.yaml")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal file name (extension has been changed) (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot rename folder with extension
    given().log().all()
            .queryParam("name", "wibble.dir")
            .post("/api/design/rename/sub1/newfolder")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Illegal folder name (from ServiceException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Cannot delete dir containing files
    given().log().all()
            .delete("/api/design/file/sub1/newfolder")
            .then().log().all()
            .statusCode(400)
            .contentType(ContentType.TEXT)
            .body(startsWith("Directory not empty (from IllegalArgumentException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Can rename dir with contents
    given().log().all()
            .queryParam("name", "newdir")
            .post("/api/design/rename/sub1/newfolder")
            .then().log().all()
            .statusCode(200)
            .contentType(equalTo("application/json"))
            .body("children[2].children[0].name", equalTo("newdir"));

    
    // Cannot delete file with its old name
    given().log().all()
            .delete("/api/design/file/sub1/newfolder/new.yaml")
            .then().log().all()
            .statusCode(404)
            .contentType(ContentType.TEXT)
            .body(startsWith("File not found (from FileNotFoundException@uk.co.spudsoft.query.web.rest.DesignHandler:"));

    // Can delete file
    given().log().all()
            .delete("/api/design/file/sub1/newdir/bob.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(not(containsString("new.yaml")));

    // Can delete folder
    given().log().all()
            .delete("/api/design/file/sub1/newdir/subfolder")
            .then().log().all()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(not(containsString("new.yaml")));

    // Can delete other file
    given().log().all()
            .delete("/api/design/file/sub1/newdir/new2.yaml")
            .then().log().all()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(not(containsString("new.yaml")));

    // Can delete empty dir
    given().log().all()
            .delete("/api/design/file/sub1/newdir")
            .then().log().all()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(not(containsString("newfolder")));

    main.shutdown();
  }

}
