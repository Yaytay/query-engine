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

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOff;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOn;
import static uk.co.spudsoft.query.main.TracingSampler.parent;
import static uk.co.spudsoft.query.main.TracingSampler.ratio;

/**
 *
 * @author jtalbut
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainTest {
  
  private static final Logger logger = LoggerFactory.getLogger(MainTest.class);
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
  
  @Test
  public void testMainExitOnRun() throws Exception {
    logger.info("testMainExitOnRun");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
            "--exitOnRun"
            , "--baseConfigPath=" + CONFS_DIR
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
    }, stdout, System.getenv());
    logger.info("testMainExitOnRun - exit");
  }
  
  @Test
  public void testMain() throws IOException {
    logger.info("testMain");
    Main.main(new String[]{
            "--baseConfigPath=" + CONFS_DIR
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
    });
    logger.info("testMain - exit");
  }
  
  @Test
  public void testFileType() {
    assertEquals("directory", Main.fileType(new File("target/classes")));
    assertEquals("does not exist", Main.fileType(new File("target/nonexistant")));
    assertEquals("file", Main.fileType(new File("pom.xml")));
  }
  
  /**
   * Test of prepareBaseConfigPath method, of class DesignMain.
   */
  @Test
  public void testPrepareBaseConfigPath() throws Exception {
    logger.info("testPrepareBaseConfigPath");
    File testDir = new File(CONFS_DIR);
    if (testDir.exists()) {
      deleteDir(testDir);
    }
    assertFalse(testDir.exists());
    
    Main.prepareBaseConfigPath(testDir, null);
    assertTrue(testDir.exists());
    assertEquals(34, countFilesInDir(testDir));
    FileTime lastMod1 = Files.getLastModifiedTime(testDir.toPath());
    Thread.sleep(1000);
    
    Main.prepareBaseConfigPath(testDir, null);
    assertTrue(testDir.exists());
    assertEquals(34, countFilesInDir(testDir));
    FileTime lastMod2 = Files.getLastModifiedTime(testDir.toPath());
    assertEquals(lastMod1, lastMod2);
    logger.info("testPrepareBaseConfigPath - exit");
  }

  private void deleteDir(File testDir) throws IOException {
    try (var dirStream = Files.walk(testDir.toPath())) {
      dirStream
              .map(Path::toFile)
              .sorted(Comparator.reverseOrder())
              .forEach(File::delete);
    }
  }

  private long countFilesInDir(File testDir) throws IOException {
    try (var dirStream = Files.walk(testDir.toPath())) {
      return dirStream
              .map(Path::toFile)
              .filter(f -> f.isFile())
              .count();
    }
  }

  @Test
  void testSamplerAlwaysOn() {
    TracingConfig config = new TracingConfig();
    config.setSampler(TracingSampler.alwaysOn);

    Sampler result = Main.sampler(config);

    assertEquals(Sampler.alwaysOn(), result);
  }

  @Test
  void testSamplerAlwaysOff() {
    TracingConfig config = new TracingConfig();
    config.setSampler(TracingSampler.alwaysOff);

    Sampler result = Main.sampler(config);

    assertEquals(Sampler.alwaysOff(), result);
  }

  @Test
  void testSamplerRatio() {
    TracingConfig config = new TracingConfig();
    config.setSampler(TracingSampler.ratio);
    config.setSampleRatio(0.5);

    Sampler result = Main.sampler(config);

    assertEquals(Sampler.traceIdRatioBased(0.5), result);
  }

  @Test
  void testSamplerParent() {
    TracingConfig config = new TracingConfig();
    config.setSampler(TracingSampler.parent);
    
    config.setRootSampler(TracingSampler.alwaysOff);
    assertEquals(Sampler.parentBased(Sampler.alwaysOff()), Main.sampler(config));
    
    config.setRootSampler(TracingSampler.alwaysOn);
    assertEquals(Sampler.parentBased(Sampler.alwaysOn()), Main.sampler(config));
    
    config.setRootSampler(TracingSampler.parent);
    
    config.setRootSampler(TracingSampler.ratio);
    config.setSampleRatio(0.7);
    assertEquals(Sampler.parentBased(Sampler.traceIdRatioBased(0.7)), Main.sampler(config));
  }

  @Test
  void testSpanExporterNone() {
    TracingConfig config = new TracingConfig();
    assertEquals(TracingProtocol.none, config.getProtocol());

    SpanExporter result = Main.spanExporter(config);

    assertNull(result);
  }

  @Test
  void testSpanExporterOtlpHttp() {
    TracingConfig config = new TracingConfig();
    config.setProtocol(TracingProtocol.otlphttp);
    assertEquals(TracingProtocol.otlphttp, config.getProtocol());

    SpanExporter result = Main.spanExporter(config);
    assertNull(result);
    
    config.setUrl("http://localhost");
    result = Main.spanExporter(config);
    assertNotNull(result);
    assertThat(result, instanceOf(OtlpHttpSpanExporter.class));
  }

  @Test
  void testSpanExporterZipkin() {
    TracingConfig config = new TracingConfig();
    config.setProtocol(TracingProtocol.zipkin);
    assertEquals(TracingProtocol.zipkin, config.getProtocol());

    SpanExporter result = Main.spanExporter(config);
    assertNull(result);
    
    config.setUrl("http://localhost");
    result = Main.spanExporter(config);
    assertNotNull(result);
    assertThat(result, instanceOf(ZipkinSpanExporter.class));
  }

}
