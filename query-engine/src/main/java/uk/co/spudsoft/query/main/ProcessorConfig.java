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
 * Configuration for specific {@link uk.co.spudsoft.query.exec.ProcessorInstance}s.
 * <P>
 * The majority of processor configuration is within the pipeline definitions, this configuration is for aspects where they
 * configuration needs to be managed by administrator rather than the pipeline designer.
 * 
 * @author jtalbut
 */
public class ProcessorConfig {
  
  private String tempDir = System.getProperty("java.io.tmpdir");
  private int inMemorySortLimitBytes = 1 << 22; // 4MB;

  /**
   * The temporary file to be used by {@link uk.co.spudsoft.query.exec.ProcessorInstance}s that require it.
   * <p>
   * The default value is the java system property &quot;java.io.tmpdir&quot;.
   * It is recommended that you change this value to something specific to the query engine.
   * 
   * @return the temporary file to be used by {@link uk.co.spudsoft.query.exec.ProcessorInstance}s that require it.
   */
  public String getTempDir() {
    return tempDir;
  }

  /**
  /**
   * The temporary file to be used by {@link uk.co.spudsoft.query.exec.ProcessorInstance}s that require it.
   * <p>
   * The default value is the java system property &quot;java.io.tmpdir&quot;.
   * It is recommended that you change this value to something specific to the query engine.
   * 
   * @param tempDir the temporary file to be used by {@link uk.co.spudsoft.query.exec.ProcessorInstance}s that require it.
   */
  public void setTempDir(String tempDir) {
    this.tempDir = tempDir;
  }

  /**
   * The approximate limit on the amount of memory that should be used by {@link uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance}.
   * <p>
   * The calculation of the memory used is not very precise - specifically there is no easy way to know how many bytes a {@link java.lang.String} uses.
   * @return the approximate limit on the amount of memory that should be used by {@link uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance}.
   */
  public int getInMemorySortLimitBytes() {
    return inMemorySortLimitBytes;
  }

  /**
   * The approximate limit on the amount of memory that should be used by {@link uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance}.
   * <p>
   * The calculation of the memory used is not very precise - specifically there is no easy way to know how many bytes a {@link java.lang.String} uses.
   * @param inMemorySortLimitBytes approximate limit on the amount of memory that should be used by {@link uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance}.
   */
  public void setInMemorySortLimitBytes(int inMemorySortLimitBytes) {
    this.inMemorySortLimitBytes = inMemorySortLimitBytes;
  }
  
}
