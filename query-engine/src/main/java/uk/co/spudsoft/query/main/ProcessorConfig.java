/*
 * Copyright (C) 2024 jtalbut
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

/**
 *
 * @author jtalbut
 */
public class ProcessorConfig {
  
  private String tempDir = System.getProperty("java.io.tmpdir");
  private int inMemorySortLimitBytes = 1 << 22; // 4MB;

  public String getTempDir() {
    return tempDir;
  }

  public void setTempDir(String tempDir) {
    this.tempDir = tempDir;
  }

  public int getInMemorySortLimitBytes() {
    return inMemorySortLimitBytes;
  }

  public void setInMemorySortLimitBytes(int inMemorySortLimitBytes) {
    this.inMemorySortLimitBytes = inMemorySortLimitBytes;
  }
  
}
