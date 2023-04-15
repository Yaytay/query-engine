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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class DesignMainTest {
  
  public static class DesignMainTester extends DesignMain {

    @Override
    protected void prepareBaseConfigPath(File baseConfigFile) {
      super.prepareBaseConfigPath(baseConfigFile);
    }
    
  }
  
  /**
   * Test of prepareBaseConfigPath method, of class DesignMain.
   */
  @Test
  public void testPrepareBaseConfigPath() throws Exception {
    File testDir = new File("target/test-base-config-path");
    if (testDir.exists()) {
      deleteDir(testDir);
    }
    assertFalse(testDir.exists());
    
    DesignMainTester instance = new DesignMainTester();
    instance.prepareBaseConfigPath(testDir);
    assertTrue(testDir.exists());
    assertEquals(10, countFilesInDir(testDir));
    FileTime lastMod1 = Files.getLastModifiedTime(testDir.toPath());
    Thread.sleep(1000);
    
    instance.prepareBaseConfigPath(testDir);
    assertTrue(testDir.exists());
    assertEquals(10, countFilesInDir(testDir));
    FileTime lastMod2 = Files.getLastModifiedTime(testDir.toPath());
    assertEquals(lastMod1, lastMod2);
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
