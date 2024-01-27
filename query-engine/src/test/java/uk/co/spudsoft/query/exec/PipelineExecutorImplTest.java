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

import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.ProcessorLimit;
import uk.co.spudsoft.query.defn.SourceTest;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorLimitInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.FormatType;
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.exec.fmts.logger.LoggingWriteStream;
import uk.co.spudsoft.query.defn.Format;

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
    PipelineExecutorImpl instance = new PipelineExecutorImpl(new FilterFactory(Collections.emptyList()), null);
    instance.validatePipeline(definition).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testCreateProcessors(Vertx vertx) {
    Pipeline definition = Pipeline.builder()
            .source(SourceTest.builder().name("test").build())
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(1).build()
                            , ProcessorLimit.builder().limit(2).build()
                    )
            )
            .build();
    PipelineExecutorImpl instance = new PipelineExecutorImpl(new FilterFactory(Collections.emptyList()), null);
    List<ProcessorInstance> results = instance.createProcessors(vertx, ctx -> {}, vertx.getOrCreateContext(), definition, null);
    assertThat(results, hasSize(2));
    assertEquals(1, ((ProcessorLimitInstance) results.get(0)).getLimit());
    assertEquals(2, ((ProcessorLimitInstance) results.get(1)).getLimit());
  }

  @Test
  public void testPrepareArguments() {
    PipelineExecutorImpl instance = new PipelineExecutorImpl(new FilterFactory(Collections.emptyList()), null);
    Map<String, ArgumentInstance> result = instance.prepareArguments(
            Arrays.asList(
                    Argument.builder().type(ArgumentType.Long).name("arg1").optional(true).defaultValue("12").build()
                    , Argument.builder().type(ArgumentType.String).name("arg2").optional(true).defaultValue("message").build()
                    , Argument.builder().type(ArgumentType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
            );
    assertEquals(3, result.size());
    assertEquals("12", result.get("arg1").getValues().get(0));
    assertEquals("message", result.get("arg2").getValues().get(0));
    assertEquals(0, result.get("arg3").getValues().size());
    
    result = instance.prepareArguments(
            Arrays.asList(
                    Argument.builder().type(ArgumentType.Long).name("arg1").defaultValue("12").build()
                    , Argument.builder().type(ArgumentType.String).name("arg2").defaultValue("message").build()
                    , Argument.builder().type(ArgumentType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
                    .add("arg1", "17")
                    .add("arg2", "second")
                    .add("arg3", "third")
            );
    assertEquals(3, result.size());
    assertEquals("17", result.get("arg1").getValues().get(0));
    assertEquals("second", result.get("arg2").getValues().get(0));
    assertEquals("third", result.get("arg3").getValues().get(0));
  }

  @Test
  @Timeout(timeUnit = TimeUnit.SECONDS, value = 60)
  public void testInitializePipeline(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline definition = Pipeline.builder()
            .source(SourceTest.builder().name("test").build())
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(3).build()
                            , ProcessorLimit.builder().limit(1).build()
                    )
            )
            .build();
    PipelineExecutorImpl instance = new PipelineExecutorImpl(new FilterFactory(Collections.emptyList()), null);
    List<ProcessorInstance> processors = instance.createProcessors(vertx, ctx -> {}, vertx.getOrCreateContext(), definition, null);
    
    Map<String, ArgumentInstance> arguments = instance.prepareArguments(
            Arrays.asList(
                    Argument.builder().type(ArgumentType.Long).name("arg1").optional(true).defaultValue("12").build()
                    , Argument.builder().type(ArgumentType.String).name("arg2").optional(true).defaultValue("message").build()
                    , Argument.builder().type(ArgumentType.String).name("arg3").optional(true).build()
            )
            , 
            MultiMap.caseInsensitiveMultiMap()
            );
    
    SourceTest sourceDefn = SourceTest.builder().name("test").rowCount(7).build();
    SourceInstance source = sourceDefn.createInstance(vertx, vertx.getOrCreateContext(), instance, "source");
    FormatDelimited destDefn = FormatDelimited.builder().build();
    FormatInstance dest = destDefn.createInstance(vertx, vertx.getOrCreateContext(), new LoggingWriteStream<>(rows -> {}));
    
    PipelineInstance pi = new PipelineInstance(arguments, null, null, source, processors, dest);
    
    instance.initializePipeline(pi);
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
    
    PipelineExecutorImpl instance = new PipelineExecutorImpl(new FilterFactory(Collections.emptyList()), null);
    
    assertEquals(FormatType.JSON, instance.getFormat(formats, drBlank).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(formats, drFormat).getType());
    assertThrows(IllegalArgumentException.class, () -> instance.getFormat(formats, drBadFormat).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(formats, drExtension).getType());
    assertThrows(IllegalArgumentException.class, () -> instance.getFormat(formats, drBadExtension).getType());
    assertEquals(FormatType.JSON, instance.getFormat(formats, drAcceptJsonOverXlsx).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(formats, drAcceptXlsxOverJson).getType());
    assertEquals(FormatType.XLSX, instance.getFormat(formats, drAcceptXlsxOverWild).getType());
    // For equally valid type matches prefer the first listed in the pipeline
    assertEquals(FormatType.JSON, instance.getFormat(formats, drAcceptWild).getType());
    // A Q that cannot be parsed as a double will be considered to be 0.0
    assertEquals(FormatType.XLSX, instance.getFormat(formats, drAcceptXlsxOverJsonWithBadQ).getType());
  }
  
}
