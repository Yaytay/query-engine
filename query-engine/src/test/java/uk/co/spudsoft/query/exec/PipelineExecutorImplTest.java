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
package uk.co.spudsoft.query.exec;

import uk.co.spudsoft.query.exec.context.RequestContext;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentValue;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.ProcessorLimit;
import uk.co.spudsoft.query.defn.SourceTest;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorLimitInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.FormatType;
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.exec.fmts.logger.LoggingWriteStream;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineExecutorImplTest {
  
  private static final Logger logger = LoggerFactory.getLogger(PipelineExecutorImplTest.class);
  
  @Test
  public void testValidatePipeline(Vertx vertx, VertxTestContext testContext) {
    Pipeline definition = Pipeline.builder()
            .source(SourceTest.builder().name("test").build())
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .build();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    PipelineExecutor instance = PipelineExecutor.create(null, auditor, new FilterFactory(Collections.emptyList()), null);
    instance.validatePipeline(definition).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testCreateProcessors(Vertx vertx) {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    Pipeline definition = Pipeline.builder()
            .source(SourceTest.builder().name("test").build())
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(1).build()
                            , ProcessorLimit.builder().limit(2).build()
                    )
            )
            .build();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    PipelineExecutor instance = PipelineExecutor.create(null, auditor, new FilterFactory(Collections.emptyList()), null);
    List<ProcessorInstance> results = instance.createProcessors(vertx, pipelineContext, definition, null);
    assertThat(results, hasSize(2));
    assertEquals(1, ((ProcessorLimitInstance) results.get(0)).getLimit());
    assertEquals(2, ((ProcessorLimitInstance) results.get(1)).getLimit());
  }

  @Test
  public void testPrepareArguments() throws Throwable {
    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , null
            , HeadersMultiMap.httpHeaders().add("Host", "localhost:123")
            , null
            , new IPAddressString("127.0.0.1")
            , null
    );
    PipelineExecutor instance = PipelineExecutor.create(null, null, new FilterFactory(Collections.emptyList()), null);
    Map<String, ArgumentInstance> result = instance.prepareArguments(
            req
            , Arrays.asList(
                    Argument.builder().type(DataType.Long).name("arg1").optional(true).defaultValueExpression("12").build()
                    , Argument.builder().type(DataType.String).name("arg2").optional(true).defaultValueExpression("'message'").build()
                    , Argument.builder().type(DataType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
            );
    assertEquals(3, result.size());
    assertEquals(12L, result.get("arg1").getValues().get(0));
    assertEquals("message", result.get("arg2").getValues().get(0));
    assertEquals(0, result.get("arg3").getValues().size());
    
    result = instance.prepareArguments(
            req
            , Arrays.asList(
                    Argument.builder().type(DataType.Long).name("arg1").defaultValueExpression("12").build()
                    , Argument.builder().type(DataType.String).name("arg2").defaultValueExpression("message").build()
                    , Argument.builder().type(DataType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
                    .add("arg1", "17")
                    .add("arg2", "second")
                    .add("arg3", "third")
            );
    assertEquals(3, result.size());
    assertEquals(17L, result.get("arg1").getValues().get(0));
    assertEquals("second", result.get("arg2").getValues().get(0));
    assertEquals("third", result.get("arg3").getValues().get(0));
  }

  @Test
  @Timeout(timeUnit = TimeUnit.SECONDS, value = 60)
  public void testInitializePipeline(Vertx vertx, VertxTestContext testContext) throws Throwable {
    
    Pipeline definition = Pipeline.builder()
            .source(SourceTest.builder().name("test").build())
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(3).build()
                            , ProcessorLimit.builder().limit(1).build()
                    )
            )
            .build();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    PipelineExecutor instance = PipelineExecutor.create(null, auditor, new FilterFactory(Collections.emptyList()), null);
    
    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , null
            , HeadersMultiMap.httpHeaders().add("Host", "localhost:123")
            , null
            , new IPAddressString("127.0.0.1")
            , null
    );
    PipelineContext pipelineContext = new PipelineContext("test", req);
    
    List<ProcessorInstance> processors = instance.createProcessors(vertx, pipelineContext, definition, null);

    Map<String, ArgumentInstance> arguments = instance.prepareArguments(
            req
            , 
            Arrays.asList(
                    Argument.builder().type(DataType.Long).name("arg1").optional(true).defaultValueExpression("12").build()
                    , Argument.builder().type(DataType.String).name("arg2").optional(true).defaultValueExpression("message").build()
                    , Argument.builder().type(DataType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
            );
    
    SourceTest sourceDefn = SourceTest.builder().name("test").rowCount(7).build();
    SourceInstance source = sourceDefn.createInstance(vertx, null, auditor, pipelineContext, instance);
    FormatDelimited destDefn = FormatDelimited.builder().build();
    FormatInstance dest = destDefn.createInstance(vertx, pipelineContext, new LoggingWriteStream<>(rows -> {}));
    
    PipelineInstance pi = new PipelineInstance(pipelineContext, definition, arguments, null, null, source, processors, dest);
    
    instance.initializePipeline(pipelineContext, pi);
    pi.getFinalPromise().future().onComplete(testContext.succeedingThenComplete());    
  }
  
  @Test
  public void testGetFormat() {
    FormatRequest drBlank = FormatRequest.builder().build();
    FormatRequest drFormat = FormatRequest
            .builder()
            .name("xlsx")
            .build();
    FormatRequest drBadFormat = FormatRequest
            .builder()
            .name("bad")
            .build();
    FormatRequest drExtension = FormatRequest
            .builder()
            .extension("xlsx")
            .build();
    FormatRequest drBadExtension = FormatRequest
            .builder()
            .extension("bad")
            .build();
    FormatRequest drAcceptWild = FormatRequest
            .builder()
            .accept(
                    Arrays.asList(
                            MediaType.parse("application/pdf")
                            , MediaType.parse("*/*; q=0.1")
                    )
            )
            .build();
    logger.debug("PDF 1.0, Wildcard 0.1: {}", drAcceptWild.getAccept());
    
    FormatRequest drAcceptXlsxOverWild = FormatRequest
            .builder()
            .accept(
                    Arrays.asList(
                            MediaType.parse("*/*")
                            , MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            , MediaType.parse("application/*")
                    )
            )
            .build();
    logger.debug("Wildcard 1.0, XLSX 1.0, Partial wildcard 1.0: {}", drAcceptXlsxOverWild.getAccept());
    
    FormatRequest drAcceptJsonOverXlsx = FormatRequest
            .builder()
            .accept(
                    Arrays.asList(
                            MediaType.parse("application/json; q=0.7")
                            , MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; q=0.1")
                    )
            )
            .build();
    logger.debug("JSON 0.7, XLSX 0.1: {}", drAcceptJsonOverXlsx.getAccept());
    
    FormatRequest drAcceptXlsxOverJson = FormatRequest
            .builder()
            .accept(
                    Arrays.asList(
                            MediaType.parse("application/json; q=0.1")
                            , MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; q=0.5")
                    )
            )
            .build();    
    logger.debug("JSON 0.1, XLSX 0.5: {}", drAcceptXlsxOverJson.getAccept());
    
    FormatRequest drAcceptXlsxOverJsonWithBadQ = FormatRequest
            .builder()
            .accept(
                    Arrays.asList(
                            MediaType.parse("application/json; q=MAXIMUM")
                            , MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; q=0.5")
                    )
            )
            .build();    
    logger.debug("JSON 0.1, XLSX 0.5: {}", drAcceptXlsxOverJson.getAccept());
    
    List<Format> formats = Arrays.asList(FormatJson.builder().build()
            , FormatXlsx.builder().build()
    );
    
    PipelineExecutor instance = PipelineExecutor.create(null, null, new FilterFactory(Collections.emptyList()), null);

    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , null
            , HeadersMultiMap.httpHeaders().add("Host", "localhost:123")
            , null
            , new IPAddressString("127.0.0.1")
            , null
    );
    PipelineContext pipelineContext = new PipelineContext("test", req);
    
    assertEquals(FormatType.JSON, instance.getFormat(pipelineContext, formats, drBlank).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(pipelineContext, formats, drFormat).getType());
    assertThrows(IllegalArgumentException.class, () -> instance.getFormat(pipelineContext, formats, drBadFormat).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(pipelineContext, formats, drExtension).getType());
    assertThrows(IllegalArgumentException.class, () -> instance.getFormat(pipelineContext, formats, drBadExtension).getType());
    assertEquals(FormatType.JSON, instance.getFormat(pipelineContext, formats, drAcceptJsonOverXlsx).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(pipelineContext, formats, drAcceptXlsxOverJson).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(pipelineContext, formats, drAcceptXlsxOverWild).getType());
    // For equally valid type matches prefer the first listed in the pipeline
    assertEquals(FormatType.JSON, instance.getFormat(pipelineContext, formats, drAcceptWild).getType());
    // A Q that cannot be parsed as a double will be considered to be 0.0
    assertEquals(FormatType.XLSX, instance.getFormat(pipelineContext, formats, drAcceptXlsxOverJsonWithBadQ).getType());
  }

  @Test
  public void testValidatePipeline() {
    Pipeline pipeline = Pipeline.builder().build();

    PipelineExecutor instance = PipelineExecutor.create(null, null, new FilterFactory(Collections.emptyList()), null);
    Future<Pipeline> future = instance.validatePipeline(pipeline);
    assertTrue(future.failed());
    assertThat(future.cause(), instanceOf(IllegalArgumentException.class));
    assertEquals("Source not specified in root pipeline", future.cause().getMessage());
  }

  @Test
  public void testPossibleValuesContains() {
    Argument arg = Argument.builder().possibleValues(
            Arrays.asList(
                    ArgumentValue.builder().label("l1").value("v1").build()
            )
    ).build();
    assertTrue(PipelineExecutorImpl.possibleValuesContains(arg, "v1"));
    assertFalse(PipelineExecutorImpl.possibleValuesContains(arg, "v2"));
  }

  @Test
  public void testAddCastItem() throws Throwable {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipctx = new PipelineContext("test", reqctx);

    ImmutableList.Builder<Comparable<?>> builder = ImmutableList.<Comparable<?>>builder();
    PipelineExecutorImpl.addCastItem(pipctx, "test", builder, DataType.Time, "12:34");
    assertEquals("The argument \"test\" was passed a value which cannot be converted to Long.", assertThrows(IllegalArgumentException.class, () -> {
      PipelineExecutorImpl.addCastItem(pipctx, "test", builder, DataType.Long, "12:34");
    }).getMessage());
    
    assertEquals(Arrays.asList(LocalTime.of(12, 34)), builder.build());
  }

  @Test
  public void testEvaluateDefaultValues() throws Throwable {
    RequestContext requestContext = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    Argument arg = Argument.builder()
            .type(DataType.Date)
            .multiValued(true)
            .defaultValueExpression("['1971-05-06', '1968-07-30', ...]")
            .build();
    
    ImmutableList<Comparable<?>> values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(LocalDate.of(1971, 5, 6), LocalDate.of(1968, 7, 30)), values);

    arg = Argument.builder()
            .type(DataType.Date)
            .multiValued(true)
            .defaultValueExpression("['1923-05-06', '2042-07-30']")
            .build();
    
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(LocalDate.of(1923, 5, 6), LocalDate.of(2042, 7, 30)), values);

    arg = Argument.builder()
            .type(DataType.Integer)
            .multiValued(true)
            .defaultValueExpression("[23, 45, null, ...]")
            .build();
    
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(23, 45), values);

    arg = Argument.builder()
            .type(DataType.Integer)
            .multiValued(true)
            .defaultValueExpression("[23, 45, null]")
            .build();
    
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(23, 45), values);

    arg = Argument.builder()
            .type(DataType.Date)
            .multiValued(true)
            .permittedValuesRegex("\\d{4}-\\d{2}-\\d{2}")
            .defaultValueExpression("['1971-05-06', '1968-07-30', ...]")
            .build();
    
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(LocalDate.of(1971, 5, 6), LocalDate.of(1968, 7, 30)), values);

    arg = Argument.builder()
            .type(DataType.Date)
            .multiValued(true)
            .validate(false)
            .permittedValuesRegex("ABCD")
            .defaultValueExpression("['1971-05-06', '1968-07-30', ...]")
            .build();
    
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(Arrays.asList(LocalDate.of(1971, 5, 6), LocalDate.of(1968, 7, 30)), values);

    arg = Argument.builder()
            .type(DataType.String)
            .name("Department")
            .description("Department")
            .optional(true)
            .hidden(true)
            .multiValued(false)
            .ignored(false)
            .validate(true)
            .emptyIsAbsent(false)
            .dependsUpon(ImmutableList.of())
            .possibleValues(ImmutableList.of())
            .defaultValueExpression("firstMatchingStringWithPrefix(request.groups, '/Department_', true)")
            .build();
    
    Jwt jwt = new Jwt(new JsonObject(), new JsonObject().put("groups", Arrays.asList("Bob", "/Department_First department")), null, null);
    requestContext.setJwt(jwt);
            
    values = PipelineExecutorImpl.evaluateDefaultValues(pipelineContext, arg, null);
    assertNotNull(values);
    assertEquals(ImmutableList.of("First department"), values);
  }

  @Test
  public void testValidateArgumentValue() {
    RequestContext requestContext = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);

    Argument arg;
    
    // No constraints and casting is handled later:
    arg = Argument.builder()
            .type(DataType.Integer)
            .multiValued(true)
            .build();
    PipelineExecutorImpl.validateArgumentValue(pipelineContext, arg, null, "fred", true);
    
    // Still no constraints because possible values is empty and casting is handled later:
    arg = Argument.builder()
            .type(DataType.Integer)
            .possibleValues(Collections.emptyList())
            .multiValued(true)
            .build();
    PipelineExecutorImpl.validateArgumentValue(pipelineContext, arg, null, "fred", true);
    
    // Now fail because value is not in possible values
    Argument argPermitsBob = Argument.builder()
            .name("testarg")
            .type(DataType.Integer)
            .possibleValues(Arrays.asList(ArgumentValue.builder().value("bob").build()))
            .multiValued(true)
            .build();
    PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsBob, null, "bob", true);
    assertEquals("The argument \"testarg\" generated a default value which is not permitted, please contact the designer."
            , assertThrows(IllegalArgumentException.class, () -> {
              PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsBob, null, "fred", true);
            }).getMessage()
            );
    assertEquals("The argument \"testarg\" was passed a value which is not permitted."
            , assertThrows(IllegalArgumentException.class, () -> {
              PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsBob, null, "fred", false);
            }).getMessage()
            );
    
    // Now fail because value is not in regex
    Argument argPermitsRegex = Argument.builder()
            .name("testarg")
            .type(DataType.Integer)
            .permittedValuesRegex("\\d+")
            .multiValued(true)
            .build();
    Pattern pattern = Pattern.compile(argPermitsRegex.getPermittedValuesRegex());
    PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsRegex, pattern, "7", true);
    assertEquals("The argument \"testarg\" generated a default value which is not permitted, please contact the designer."
            , assertThrows(IllegalArgumentException.class, () -> {
              PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsRegex, pattern, "fred", true);
            }).getMessage()
            );
    assertEquals("The argument \"testarg\" was passed a value which is not permitted."
            , assertThrows(IllegalArgumentException.class, () -> {
              PipelineExecutorImpl.validateArgumentValue(pipelineContext, argPermitsRegex, pattern, "fred", false);
            }).getMessage()
            );
    
    
    
  }

}
