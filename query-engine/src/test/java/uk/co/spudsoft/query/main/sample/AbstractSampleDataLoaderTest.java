/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.main.sample;

import io.vertx.junit5.VertxExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class AbstractSampleDataLoaderTest {
  
  @Test
  public void testCheckStaleLockfile(TestInfo testInfo) throws Exception {
    
    String testFile = "target/" + this.getClass().getSimpleName() + "_" + testInfo.getDisplayName();
    Path testPath = Path.of(testFile);
    
    Files.deleteIfExists(testPath);
    
    Files.createFile(testPath);
    
    assertFalse(AbstractSampleDataLoader.checkStaleLockFile(testPath, 1000));
    Thread.sleep(100);
    assertTrue(AbstractSampleDataLoader.checkStaleLockFile(testPath, 50));
  }
}
