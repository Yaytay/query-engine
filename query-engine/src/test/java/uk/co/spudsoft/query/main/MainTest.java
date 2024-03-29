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

import io.opentelemetry.api.GlobalOpenTelemetry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class MainTest {
  
  private static final Logger logger = LoggerFactory.getLogger(MainTest.class);
  
  @BeforeAll
  public static void createDirs() {
    File paramsDir = new File("target/query-engine/samples-maintest");
    paramsDir.mkdirs();
  }
  
  @Test
  public void testMainExitOnRun() throws Exception {
    logger.info("testMainExitOnRun");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
            "--exitOnRun"
            , "--baseConfigPath=target/query-engine/samples-maintest"
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
            , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
    }, stdout);
    logger.info("testMainExitOnRun - exit");
    GlobalOpenTelemetry.resetForTest();
  }
  
  @Test
  public void testMain() throws IOException {
    logger.info("testMain");
    Main.main(new String[]{
            "--baseConfigPath=target/query-engine/samples-maintest"
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
    });
    logger.info("testMain - exit");
    GlobalOpenTelemetry.resetForTest();
  }
  
  @Test
  public void testFileType() {
    assertEquals("directory", Main.fileType(new File("target/classes")));
    assertEquals("does not exist", Main.fileType(new File("target/nonexistant")));
    assertEquals("file", Main.fileType(new File("target/classes/logback.xml")));
  }
  
  /**
   * Test of prepareBaseConfigPath method, of class DesignMain.
   */
  @Test
  public void testPrepareBaseConfigPath() throws Exception {
    logger.info("testPrepareBaseConfigPath");
    File testDir = new File("target/test-base-config-path");
    if (testDir.exists()) {
      deleteDir(testDir);
    }
    assertFalse(testDir.exists());
    
    Main.prepareBaseConfigPath(new File("target/classes/logback.xml"), null);
    
    Main.prepareBaseConfigPath(testDir, null);
    assertTrue(testDir.exists());
    assertEquals(31, countFilesInDir(testDir));
    FileTime lastMod1 = Files.getLastModifiedTime(testDir.toPath());
    Thread.sleep(1000);
    
    Main.prepareBaseConfigPath(testDir, null);
    assertTrue(testDir.exists());
    assertEquals(31, countFilesInDir(testDir));
    FileTime lastMod2 = Files.getLastModifiedTime(testDir.toPath());
    assertEquals(lastMod1, lastMod2);
    logger.info("testPrepareBaseConfigPath - exit");
    GlobalOpenTelemetry.resetForTest();
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
    
}
